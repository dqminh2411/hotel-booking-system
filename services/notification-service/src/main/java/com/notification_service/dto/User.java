package com.notification_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record User(
    String userId,
    @NotBlank String name,
    @Email @NotBlank String email
) {
}
