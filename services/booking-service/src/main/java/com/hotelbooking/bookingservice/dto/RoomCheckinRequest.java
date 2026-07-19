package com.hotelbooking.bookingservice.dto;

import java.util.List;
import java.util.UUID;

public record RoomCheckinRequest(
    UUID hotelId,
    List<UUID> roomIds,
    List<RoomTypeQuantity> roomTypeQuantities
) {
    public record RoomTypeQuantity(UUID roomTypeId, Integer quantity) {}
}
