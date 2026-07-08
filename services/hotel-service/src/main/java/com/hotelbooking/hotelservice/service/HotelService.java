package com.hotelbooking.hotelservice.service;

import com.hotelbooking.hotelservice.dto.HotelSearchItemDTO;
import com.hotelbooking.hotelservice.dto.request.HotelSearchRequest;
import com.hotelbooking.hotelservice.dto.response.*;
import jakarta.ws.rs.BadRequestException;

import java.time.LocalDate;
import java.util.List;

public interface HotelService {

    // Comment tạm cho method cũ
    PagedResponse<HotelSummaryResponse> searchHotels(String name, String address, int page, int size);

    HotelDetailsResponse getHotelById(String hotelId);

    List<RoomTypeResponse> getRoomTypesByHotel(String hotelId, LocalDate checkin, LocalDate checkout);

    RoomTypeResponse getRoomTypeById(String hotelId, String roomTypeId, LocalDate checkin, LocalDate checkout);

    HotelAndRoomTypesResponse getRequestedRoomTypesByHotel(String hotelId, List<String> roomTypeList);

    public PagedResponse<HotelSearchItemDTO> search(HotelSearchRequest request) ;
}

