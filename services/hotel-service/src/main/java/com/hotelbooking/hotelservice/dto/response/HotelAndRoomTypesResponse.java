package com.hotelbooking.hotelservice.dto.response;
import com.hotelbooking.hotelservice.dto.Hotel;

import java.util.List;

public record HotelAndRoomTypesResponse(
    Hotel hotel,
    List<RoomTypeQuantityResponse> roomTypes
) {
}