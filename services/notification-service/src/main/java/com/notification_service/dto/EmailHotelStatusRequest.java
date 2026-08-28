package com.notification_service.dto;

import jakarta.validation.constraints.NotBlank;

public record EmailHotelStatusRequest(
    User tenant,
    Hotel hotel,
    @NotBlank
    String eventType,
    
    String reason
) {

}
