package com.hotelbooking.bookingservice.dto;

import java.util.UUID;

public record ActiveBookingRoomType(UUID roomTypeId, long bookingCount) {
}
