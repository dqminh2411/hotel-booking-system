package com.notification_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record EmailRequest(
    @Email @NotBlank String to,
    @NotBlank String sagaId,
    @NotBlank String eventType,
    String bookingId,
    BookingInfo booking,
    String reason
) {
    public record BookingInfo(
        String bookingId,
        User customer,
        String checkin,
        String checkout,
        Integer numAdults,
        Double totalAmount,
        Hotel hotel,
        List<RoomType> roomTypeList
    ) {
    }
}
