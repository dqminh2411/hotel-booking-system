package com.hotelbooking.hotelservice.dto;

import java.util.UUID;

public record RoomTypeBookingCount(UUID roomTypeId, Long bookingCount) {
}
