package com.notification_service.service;

import com.notification_service.dto.BroadcastAcceptedResponse;
import com.notification_service.dto.CreateBroadcastRequest;
import com.notification_service.dto.DeviceTokenResponse;
import com.notification_service.dto.NotificationPageResponse;
import com.notification_service.dto.NotificationResponse;
import com.notification_service.dto.UpsertDeviceTokenRequest;
import com.notification_service.enums.NotificationEventType;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    public DeviceTokenResponse upsertDeviceToken(
        UUID userId,
        UpsertDeviceTokenRequest request
    ) {
        // TODO
        return null;
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
