package com.place_booking_service.dto;

import java.util.UUID;

public record HotelSummaryResponse(UUID hotelId, String name, String address) {
}
