package com.hotelbooking.hotelservice.service;

import com.hotelbooking.hotelservice.dto.response.HotelDetailsResponse;
import com.hotelbooking.hotelservice.dto.response.RoomTypeResponse;

import com.hotelbooking.hotelservice.dto.HotelSearchItemDTO;
import com.hotelbooking.hotelservice.dto.request.HotelSearchRequest;
import com.hotelbooking.hotelservice.dto.response.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface HotelService {

    HotelDetailsResponse getHotelDetail(UUID hotelId, LocalDate checkinDate, LocalDate checkoutDate, Integer guestNum, Integer roomNum);
    List<RoomTypeResponse> getListRoomTypeByHotelId(UUID hotelId);

    HotelAndRoomTypesResponse getRequestedRoomTypesByHotel(UUID hotelId, List<UUID> roomTypeList);

    // List<RoomTypeResponse> getRoomTypesByHotel(UUID hotelId, LocalDate checkin, LocalDate checkout);

    // RoomTypeResponse getRoomTypeById(UUID hotelId, UUID roomTypeId, LocalDate checkin, LocalDate checkout);


    public PagedResponse<HotelSearchItemDTO> search(HotelSearchRequest request) ;
}

