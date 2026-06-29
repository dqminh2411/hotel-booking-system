package com.hotelbooking.hotelservice.dto;
import java.util.List;

public record HotelAndRoomTypesResponse(
    Hotel hotel,
    List<RoomTypeQuantityResponse> roomTypes
) {
}