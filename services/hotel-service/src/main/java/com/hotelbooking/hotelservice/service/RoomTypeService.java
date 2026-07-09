package com.hotelbooking.hotelservice.service;

import java.util.UUID;

import com.hotelbooking.hotelservice.dto.response.RoomTypeDetailResponse;

public interface RoomTypeService {
    RoomTypeDetailResponse getRoomTypeDetail(UUID roomTypeId);
}
