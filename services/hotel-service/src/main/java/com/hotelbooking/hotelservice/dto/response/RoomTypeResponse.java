package com.hotelbooking.hotelservice.dto.response;

import java.math.BigDecimal;

public record RoomTypeResponse(
        String roomTypeId,
        String name,
        BigDecimal basePricePerNight,
        Integer maxGuests,
        Integer bedCounts,
        Integer area,
        String coverImageUrl,
        Integer totalRooms,
        Integer availableRooms
) {
}

