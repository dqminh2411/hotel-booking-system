package com.hotelbooking.hotelservice.service;

import com.hotelbooking.hotelservice.dto.response.HotelDetailsResponse;

import java.time.LocalDate;
import java.util.UUID;

public interface HotelService {

    HotelDetailsResponse getHotelDetail(UUID hotelId, LocalDate checkinDate, LocalDate checkoutDate, Integer guestNum, Integer roomNum);
}

