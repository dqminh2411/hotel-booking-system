package com.notification_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.notification_service.dto.BroadcastAcceptedResponse;
import com.notification_service.dto.CreateBroadcastRequest;
import com.notification_service.dto.DeviceTokenResponse;
import com.notification_service.dto.NotificationPageResponse;
import com.notification_service.dto.NotificationResponse;
import com.notification_service.dto.RevokeDeviceTokenRequest;
import com.notification_service.dto.TopicSubscriptionRequest;
import com.notification_service.dto.TopicSubscriptionResponse;
import com.notification_service.dto.UpsertDeviceTokenRequest;
import com.notification_service.entity.DeviceTokenEntity;
import com.notification_service.enums.NotificationEventType;
import com.notification_service.repository.DeviceTokenRepository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
@AllArgsConstructor
public class NotificationService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final FcmPushService fcmPushService;

    @Transactional
    public DeviceTokenResponse upsertDeviceToken(
            UUID userId,
            UpsertDeviceTokenRequest request) {
        Instant now = Instant.now();

        deviceTokenRepository.findByFcmTokenAndActiveTrue(request.fcmToken())
                .ifPresent(activeToken -> {
                    if (!activeToken.getUserId().equals(userId)) {
                        throw new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "FCM token đang thuộc về một người dùng khác.");
                    }
                });

        DeviceTokenEntity token = deviceTokenRepository
                .findFirstByFcmTokenAndUserIdOrderByUpdatedAtDesc(request.fcmToken(), userId)
                .orElseGet(() -> DeviceTokenEntity.builder()
                        .userId(userId)
                        .fcmToken(request.fcmToken())
                        .createdAt(now)
                        .build());

        token.setPlatform(request.platform());
        token.setActive(true);
        token.setInvalidatedAt(null);
        token.setUpdatedAt(now);

        DeviceTokenEntity savedToken = deviceTokenRepository.save(token);
        return new DeviceTokenResponse(
                savedToken.getId(),
                savedToken.getUserId(),
                savedToken.getFcmToken(),
                savedToken.getPlatform(),
                savedToken.getActive(),
                savedToken.getCreatedAt(),
                savedToken.getUpdatedAt());
    }

    @Transactional
    public void revokeDeviceToken(UUID userId, RevokeDeviceTokenRequest request) {
        Instant now = Instant.now();
        deviceTokenRepository.findByUserIdAndFcmTokenAndActiveTrue(userId, request.fcmToken())
                .ifPresent(token -> {
                    token.setActive(false);
                    token.setInvalidatedAt(now);
                    token.setUpdatedAt(now);
                    deviceTokenRepository.save(token);
                });
    }

    public TopicSubscriptionResponse subscribeTopic(UUID userId, TopicSubscriptionRequest request) {
        fcmPushService.subscribeToTopic(List.of(request.fcmToken()), request.topic());
        return new TopicSubscriptionResponse(
                userId,
                request.fcmToken(),
                request.topic(),
                true,
                "Đăng ký nhận thông báo topic thành công.");
    }

    public TopicSubscriptionResponse unsubscribeTopic(UUID userId, TopicSubscriptionRequest request) {
        fcmPushService.unsubscribeFromTopic(List.of(request.fcmToken()), request.topic());
        return new TopicSubscriptionResponse(
                userId,
                request.fcmToken(),
                request.topic(),
                false,
                "Hủy đăng ký nhận thông báo topic thành công.");
    }

    public NotificationPageResponse listNotifications(
            UUID userId,
            boolean unreadOnly,
            NotificationEventType eventType,
            int page,
            int size) {
        // TODO
        return null;
    }

    public NotificationResponse markAsRead(UUID notificationId, UUID userId) {
        // TODO
        return null;
    }

    public BroadcastAcceptedResponse validateAndInitiateBroadcast(
            CreateBroadcastRequest request,
            UUID userId,
            String role) {
        // TODO
        return null;
    }
}
