package com.notification_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record User(
    UUID userId,
    @NotBlank String name,
    @Email @NotBlank String email
) {
}
