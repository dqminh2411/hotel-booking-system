package com.hotelbooking.hotelservice.dto.response;

import com.hotelbooking.hotelservice.dto.RoomTypeBookingCount;

import java.util.List;

public record BookingCountResponse(List<RoomTypeBookingCount> activeBookingCount) {
}
