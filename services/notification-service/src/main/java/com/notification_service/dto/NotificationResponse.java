package com.notification_service.dto;

import com.notification_service.enums.NotificationEventType;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationResponse(
    UUID id,
    UUID recipientUserId,
    NotificationEventType eventType,
    String title,
    String body,
    Map<String, Object> payload,
    Instant readAt,
    Instant createdAt
) {
}
