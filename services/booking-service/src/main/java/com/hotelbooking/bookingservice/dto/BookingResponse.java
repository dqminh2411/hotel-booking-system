package com.hotelbooking.bookingservice.dto;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.hotelbooking.bookingservice.enums.BookingStatus;

public record BookingResponse(
    UUID bookingId,
    BookingStatus status,
    JsonNode details
) {
}
