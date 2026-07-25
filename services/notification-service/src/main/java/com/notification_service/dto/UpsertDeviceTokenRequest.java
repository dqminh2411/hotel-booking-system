package com.notification_service.dto;

import com.notification_service.enums.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpsertDeviceTokenRequest(
    @NotBlank @Size(min = 20, max = 500) String fcmToken,
    @NotNull DevicePlatform platform
) {
}
