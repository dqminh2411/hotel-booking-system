package com.place_booking_service.dto;

import java.util.UUID;

public record ActiveBookingRoomType(UUID roomTypeId, long bookingCount) {
}