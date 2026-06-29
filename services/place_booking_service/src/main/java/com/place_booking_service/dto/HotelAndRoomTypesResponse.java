package com.place_booking_service.dto;

import java.util.List;

public record HotelAndRoomTypesResponse(
    Hotel hotel,
    List<RoomTypeQuantityResponse> roomTypes
) {
}
