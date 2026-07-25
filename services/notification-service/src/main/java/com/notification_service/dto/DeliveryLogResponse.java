package com.notification_service.dto;

import com.notification_service.enums.DeliveryStatus;
import com.notification_service.enums.NotificationChannel;
import java.time.Instant;
import java.util.UUID;

public record DeliveryLogResponse(
    UUID id,
    UUID notificationId,
    NotificationChannel channel,
    DeliveryStatus status,
    Integer retryCount,
    Integer maxRetries,
    String errorMessage,
    Instant sentAt,
    Instant createdAt,
    Instant updatedAt
) {
}
