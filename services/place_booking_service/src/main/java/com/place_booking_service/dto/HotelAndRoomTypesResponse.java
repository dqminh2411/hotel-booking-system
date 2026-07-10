package com.place_booking_service.dto;

import java.util.List;

public record HotelAndRoomTypesResponse(
    HotelSummaryResponse hotel,
    List<RoomTypeQuantityResponse> roomTypes
) {
}
