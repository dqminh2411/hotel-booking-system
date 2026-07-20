package com.hotelbooking.bookingservice.dto;

import java.util.List;
import java.util.UUID;

import com.hotelbooking.bookingservice.enums.RoomStatus;

public record RoomCheckinRequest(
    List<UUID> roomIds,
    List<RoomTypeQuantity> roomTypeQuantities, 
    RoomStatus oldStatus,
    RoomStatus newStatus
) {
    public record RoomTypeQuantity(UUID roomTypeId, Integer quantity) {}
}
