package com.hotelbooking.hotelservice.dto;
import java.util.UUID;

public record HotelSummaryResponse(UUID hotelId, String name, String address) {
}

