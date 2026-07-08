package com.notification_service.service;

import com.notification_service.dto.BroadcastAcceptedResponse;
import com.notification_service.dto.CreateBroadcastRequest;
import com.notification_service.dto.DeviceTokenResponse;
import com.notification_service.dto.NotificationPageResponse;
import com.notification_service.dto.NotificationResponse;
import com.notification_service.dto.UpsertDeviceTokenRequest;
import com.notification_service.entity.DeviceTokenEntity;
import com.notification_service.enums.NotificationEventType;
import com.notification_service.repository.DeviceTokenRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationService {

    private final DeviceTokenRepository deviceTokenRepository;

    public NotificationService(DeviceTokenRepository deviceTokenRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
    }

    @Transactional
    public DeviceTokenResponse upsertDeviceToken(
        UUID userId,
        UpsertDeviceTokenRequest request
    ) {
        Instant now = Instant.now();
        DeviceTokenEntity token = deviceTokenRepository.findByFcmToken(request.fcmToken())
            .map(existingToken -> {
                if (!existingToken.getUserId().equals(userId)) {
                    throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "FCM token đã thuộc về người dùng khác."
                    );
                }
                existingToken.setPlatform(request.platform());
                existingToken.setActive(true);
                existingToken.setInvalidatedAt(null);
                existingToken.setUpdatedAt(now);
                return existingToken;
            })
            .orElseGet(() -> DeviceTokenEntity.builder()
                .userId(userId)
                .fcmToken(request.fcmToken())
                .platform(request.platform())
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build());

        DeviceTokenEntity savedToken = deviceTokenRepository.save(token);
        return new DeviceTokenResponse(
            savedToken.getId(),
            savedToken.getUserId(),
            savedToken.getFcmToken(),
            savedToken.getPlatform(),
            savedToken.getActive(),
            savedToken.getCreatedAt(),
            savedToken.getUpdatedAt()
        );
    }

    public NotificationPageResponse listNotifications(
        UUID userId,
        boolean unreadOnly,
        NotificationEventType eventType,
        int page,
        int size
    ) {
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
        String role
    ) {
        // TODO
        return null;
    }
}
