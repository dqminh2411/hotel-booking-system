package com.hotelbooking.hotelservice.service;

import com.hotelbooking.hotelservice.dto.response.HotelDetailsResponse;
import com.hotelbooking.hotelservice.dto.response.RoomTypeResponse;

import com.hotelbooking.hotelservice.dto.HotelSearchItemDTO;
import com.hotelbooking.hotelservice.dto.request.HotelSearchRequest;
import com.hotelbooking.hotelservice.dto.response.*;
import jakarta.ws.rs.BadRequestException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface HotelService {

    HotelDetailsResponse getHotelDetail(UUID hotelId, LocalDate checkinDate, LocalDate checkoutDate, Integer guestNum, Integer roomNum);



    List<RoomTypeResponse> getListRoomTypeByHotelId(UUID hotelId);
    List<RoomTypeResponse> getRoomTypesByHotel(String hotelId, LocalDate checkin, LocalDate checkout);

    RoomTypeResponse getRoomTypeById(String hotelId, String roomTypeId, LocalDate checkin, LocalDate checkout);

//    HotelAndRoomTypesResponse getRequestedRoomTypesByHotel(String hotelId, List<String> roomTypeList);

    @Transactional(readOnly = true)
    HotelAndRoomTypesResponse getRequestedRoomTypesByHotel(UUID hotelId, List<UUID> roomTypeList);

    public PagedResponse<HotelSearchItemDTO> search(HotelSearchRequest request) ;
}

