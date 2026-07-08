package com.hotelbooking.hotelservice.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record RoomTypeResponse(
        UUID roomTypeId,
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

