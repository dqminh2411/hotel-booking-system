package com.hotelbooking.userservice.service;

import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class NotificationDeviceTokenClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String notificationServiceUrl;

    public NotificationDeviceTokenClient(
            @Value("${app.notification-service.url:http://notification-service:5000}") String notificationServiceUrl
    ) {
        this.notificationServiceUrl = notificationServiceUrl.replaceAll("/+$", "");
    }

    public void revokeDeviceToken(UUID userId, String fcmToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", userId.toString());

        HttpEntity<Map<String, String>> request = new HttpEntity<>(
                Map.of("fcmToken", fcmToken),
                headers
        );

        restTemplate.postForEntity(
                notificationServiceUrl + "/api/notifications/device-token/revoke",
                request,
                Void.class
        );
    }
}
