package com.hotelbooking.bookingservice.dto.kafka;

import java.util.UUID;

public record CancelBooking(
    UUID sagaId,
    String eventType,
    UUID bookingId,
    String reason
) {
}

