package com.notification_service.dto;

import com.notification_service.enums.DevicePlatform;
import java.time.Instant;
import java.util.UUID;

public record DeviceTokenResponse(
    UUID id,
    UUID userId,
    String fcmToken,
    DevicePlatform platform,
    Boolean isActive,
    Instant createdAt,
    Instant updatedAt
) {
}
