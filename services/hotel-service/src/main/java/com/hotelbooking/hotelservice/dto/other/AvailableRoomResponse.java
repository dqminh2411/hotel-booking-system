package com.hotelbooking.hotelservice.dto.other;

import java.util.UUID;

public record AvailableRoomResponse(
    UUID roomId,
    String roomNumber,
    Integer floor
) {

}
