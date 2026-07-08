package com.hotelbooking.bookingservice.dto.kafka;

import java.util.UUID;

public record UpdateBookingCommand(
    String eventType,
    UUID bookingId
) {
}
