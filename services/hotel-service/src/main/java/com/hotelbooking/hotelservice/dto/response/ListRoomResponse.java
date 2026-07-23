package com.hotelbooking.hotelservice.dto.response;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.hotelbooking.hotelservice.dto.other.AvailableRoomResponse;

public record ListRoomResponse(
    Map<UUID, List<AvailableRoomResponse>> listRoomResponse
) {

}
