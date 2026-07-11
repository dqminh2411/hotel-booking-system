package com.hotelbooking.bookingservice.dto.kafka;

import java.util.UUID;

public record ConfirmBooking(
    UUID sagaId,
    String eventType,
    UUID bookingId
) {
}

