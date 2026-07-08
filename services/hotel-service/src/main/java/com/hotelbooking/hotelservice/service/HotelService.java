package com.hotelbooking.hotelservice.service;

import com.hotelbooking.hotelservice.dto.response.HotelAndRoomTypesResponse;
import com.hotelbooking.hotelservice.dto.response.HotelDetailsResponse;
import com.hotelbooking.hotelservice.dto.response.RoomTypeResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface HotelService {

    HotelDetailsResponse getHotelDetail(UUID hotelId, LocalDate checkinDate, LocalDate checkoutDate, Integer guestNum, Integer roomNum);

    List<RoomTypeResponse> getListRoomTypeByHotelId(UUID hotelId);

    HotelAndRoomTypesResponse getRequestedRoomTypesByHotel(UUID hotelId, List<UUID> roomTypeList);
}

