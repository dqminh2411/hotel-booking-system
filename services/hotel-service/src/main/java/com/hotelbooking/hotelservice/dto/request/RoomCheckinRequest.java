package com.hotelbooking.hotelservice.dto.request;

import java.util.List;
import java.util.UUID;

import com.hotelbooking.hotelservice.constant.RoomStatus;

public record RoomCheckinRequest(
    List<UUID> roomIds,
    List<RoomTypeQuantity> roomTypeQuantities, 
    RoomStatus oldStatus,
    RoomStatus newStatus
) {
    public record RoomTypeQuantity(UUID roomTypeId, Integer quantity) {}
}
