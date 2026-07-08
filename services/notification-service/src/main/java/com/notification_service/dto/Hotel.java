package com.notification_service.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record Hotel(
    UUID hotelId,
    @NotBlank String name,
    String address
) {
}
