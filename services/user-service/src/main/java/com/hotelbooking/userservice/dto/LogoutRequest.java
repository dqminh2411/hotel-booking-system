package com.hotelbooking.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LogoutRequest(
        @NotBlank @Size(min = 20, max = 500) String fcmToken
) {
}
