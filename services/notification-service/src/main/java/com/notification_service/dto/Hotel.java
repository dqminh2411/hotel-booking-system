package com.notification_service.dto;

import jakarta.validation.constraints.NotBlank;

public record Hotel(
    String hotelId,
    @NotBlank String name,
    String address
) {
}
