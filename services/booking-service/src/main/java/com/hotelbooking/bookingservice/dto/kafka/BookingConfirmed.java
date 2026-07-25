package com.hotelbooking.bookingservice.dto.kafka;

import java.util.UUID;

import com.hotelbooking.bookingservice.dto.BookingDetail;

public record BookingConfirmed(
    UUID sagaId,
    String eventType,
    BookingDetail booking
) {
}

