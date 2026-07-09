package com.hotelbooking.hotelservice.dto.response;
import java.util.UUID;

public record HotelSummaryResponse(UUID hotelId, String name, String address) {
}

