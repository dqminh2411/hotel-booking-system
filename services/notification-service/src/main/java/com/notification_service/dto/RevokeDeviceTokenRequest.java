package com.notification_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RevokeDeviceTokenRequest(
    @NotBlank @Size(min = 20, max = 500) String fcmToken
) {
}
