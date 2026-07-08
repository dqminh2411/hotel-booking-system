package com.notification_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record EmailRequest(
    @Email @NotBlank String to,
    UUID sagaId,
    @NotBlank String eventType,
    UUID bookingId,
    BookingInfo booking,
    String reason
) {
    public record BookingInfo(
        UUID bookingId,
        User customer,
        String checkin,
        String checkout,
        Integer numAdults,
        BigDecimal totalAmount,
        Hotel hotel,
        List<RoomType> roomTypeList
    ) {
    }
}
