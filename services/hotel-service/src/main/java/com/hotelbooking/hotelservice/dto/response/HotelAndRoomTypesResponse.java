package com.hotelbooking.hotelservice.dto.response;
import java.util.List;

public record HotelAndRoomTypesResponse(
    HotelSummaryResponse hotel,
    List<RoomTypeQuantityResponse> roomTypes
) {
}
