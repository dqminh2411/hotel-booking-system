package com.hotelbooking.hotelservice.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import com.hotelbooking.hotelservice.constant.RoomStatus;
import com.hotelbooking.hotelservice.entity.RoomEntity;

public interface RoomRepository extends JpaRepository<RoomEntity, UUID>{

    List<RoomEntity> findByRoomType_IdInAndStatus(List<UUID> roomTypeIds, RoomStatus status);
}
