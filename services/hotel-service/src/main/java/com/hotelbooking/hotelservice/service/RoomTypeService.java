package com.hotelbooking.hotelservice.service;

import java.util.List;
import java.util.UUID;

import com.hotelbooking.hotelservice.dto.response.ListRoomResponse;
import com.hotelbooking.hotelservice.dto.response.RoomTypeDetailResponse;

public interface RoomTypeService {
    RoomTypeDetailResponse getRoomTypeDetail(UUID roomTypeId);

    ListRoomResponse getListRoomAvailable(List<UUID> listRoomTypeIdRequest);
}
