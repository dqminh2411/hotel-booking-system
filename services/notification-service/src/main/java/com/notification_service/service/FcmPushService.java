package com.notification_service.service;

import com.google.api.core.ApiFuture;
import com.google.api.gax.rpc.Batch;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import com.notification_service.entity.DeviceTokenEntity;
import com.notification_service.repository.DeviceTokenRepository;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class FcmPushService {

    private static final Logger log = LoggerFactory.getLogger(FcmPushService.class);

    private final DeviceTokenRepository deviceTokenRepository;
    private final String serviceAccountJsonPath;
    private FirebaseMessaging firebaseMessaging;

    public FcmPushService(
            DeviceTokenRepository deviceTokenRepository,
            @Value("${firebase.service-account-json-path:}") String serviceAccountJsonPath) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.serviceAccountJsonPath = serviceAccountJsonPath;
    }

    public void sendBookingUpdate(
            UUID userId,
            String title,
            String body,
            Map<String, String> data) {
        // get FCM tokens
        List<String> tokens = deviceTokenRepository.findByUserIdAndActiveTrue(userId)
                .stream()
                .map(DeviceTokenEntity::getFcmToken)
                .toList();

        if (tokens.isEmpty()) {
            log.info("No active FCM tokens for user {}", userId);
            return;
        }

        // get Firebase messaging
        FirebaseMessaging messaging = getFirebaseMessaging();
        if (messaging == null) {
            log.warn("Fail to get FirebaseMessaging ");
            return;
        }

        Map<String, String> pushData = new HashMap<>(data);
        pushData.put("title", title);
        pushData.put("body", body);

        // create multicast messages (to multiple devices - each device is identified by
        // its token)
        MulticastMessage message = MulticastMessage.builder()
                .putAllData(pushData)
                .addAllTokens(tokens)
                .build();

        ApiFuture<BatchResponse> future = messaging.sendEachForMulticastAsync(message);

        Map<String, Object> info = new HashMap<>();
        info.put("userId", userId);
        info.put("tokens", tokens);

        handleFcmResult("BOOKING_UPDATE", future, info);
    }

    // gửi notification theo topic
    public void sendTopicNotification(
            String topic,
            String title,
            String body,
            Map<String, String> data) {
        FirebaseMessaging messaging = getFirebaseMessaging();
        if (messaging == null) {
            log.warn("Fail to get FirebaseMessaging for topic notification {}", topic);
            return;
        }

        data.put("title", title);
        data.put("body", body);

        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        Message message = Message.builder()
                .setTopic(topic)
                .setNotification(notification)
                .putAllData(data)
                .build();

        // send async message
        ApiFuture<String> future = messaging.sendAsync(message);
        handleFcmResult("TOPIC_NOTIFICATION", future, null);
    }

    // handle async fcm result
    @Async
    public <T> void handleFcmResult(String event, ApiFuture<T> future, Map<String, Object> data) {
        try {
            T result = future.get();
            if ("TOPIC_NOTIFICATION".equals(event))
                log.info("FCM result: {}", result);
            else if ("BOOKING_UPDATE".equals(event)) {
                BatchResponse response = (BatchResponse) result;
                UUID userId = (UUID) data.get("userId");
                List<String> tokens = (List<String>) data.get("tokens");
                log.info(
                        "FCM notification sent to user {}: {} succeeded, {} failed",
                        userId,
                        response.getSuccessCount(),
                        response.getFailureCount());
                for (int i = 0; i < response.getResponses().size(); i++) {
                    SendResponse sendResponse = response.getResponses().get(i);
                    if (!sendResponse.isSuccessful()) {
                        FirebaseMessagingException ex = sendResponse.getException();
                        log.warn("FCM token failed. userId={}, tokenIndex={}, errorCode={}, message={}, token={}",
                                userId,
                                i,
                                ex.getMessagingErrorCode(),
                                ex.getMessage(),
                                tokens.get(i));
                    }
                }
            }
        } catch (Exception error) {
            log.error("Could not handle FCM result", error);
        }
    }

    // subscribe to notification topic
    public void subscribeToTopic(List<String> tokens, String topic) {
        FirebaseMessaging messaging = getFirebaseMessaging();
        if (messaging == null) {
            log.warn("Fail to get FirebaseMessaging to subscribe tokens to topic {}", topic);
            return;
        }
        try {
            messaging.subscribeToTopic(tokens, topic);
            log.info("Successfully subscribed {} tokens to topic {}", tokens.size(), topic);
        } catch (Exception error) {
            log.error("Failed to subscribe tokens to topic {}", topic, error);
        }
    }

    // unsubscribe to notification topic
    public void unsubscribeFromTopic(List<String> tokens, String topic) {
        FirebaseMessaging messaging = getFirebaseMessaging();
        if (messaging == null) {
            log.warn("Fail to get FirebaseMessaging to unsubscribe tokens from topic {}", topic);
            return;
        }
        try {
            messaging.unsubscribeFromTopic(tokens, topic);
            log.info("Successfully unsubscribed {} tokens from topic {}", tokens.size(), topic);
        } catch (Exception error) {
            log.error("Failed to unsubscribe tokens from topic {}", topic, error);
        }
    }

    private synchronized FirebaseMessaging getFirebaseMessaging() {
        if (firebaseMessaging != null) {
            return firebaseMessaging;
        }
        if (serviceAccountJsonPath == null || serviceAccountJsonPath.isBlank()) {
            return null;
        }

        try {
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                    new FileInputStream(serviceAccountJsonPath));
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();
            FirebaseApp app = FirebaseApp.getApps().isEmpty()
                    ? FirebaseApp.initializeApp(options)
                    : FirebaseApp.getInstance();
            firebaseMessaging = FirebaseMessaging.getInstance(app);
            return firebaseMessaging;
        } catch (IOException error) {
            throw new IllegalStateException("FIREBASE_SERVICE_ACCOUNT_JSON_PATH is invalid", error);
        }
    }
}
