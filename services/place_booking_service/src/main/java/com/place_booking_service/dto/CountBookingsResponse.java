package com.place_booking_service.dto;

import java.time.LocalDate;
import java.util.List;

public record CountBookingsResponse(
    String hotelId,
    LocalDate checkin,
    LocalDate checkout,
    List<ActiveBookingRoomType> activeBookingCount
) {
}
