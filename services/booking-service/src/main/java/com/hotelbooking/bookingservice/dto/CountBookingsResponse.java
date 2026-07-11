package com.hotelbooking.bookingservice.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CountBookingsResponse(
    UUID hotelId,
    LocalDate checkin,
    LocalDate checkout,
    List<ActiveBookingRoomType> activeBookingCount
) {
}
