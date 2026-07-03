package com.hotelbooking.hotelservice.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hotelbooking.hotelservice.entity.RoomTypeImageEntity;

public interface RoomTypeImageRepository extends JpaRepository<RoomTypeImageEntity, UUID>{
    List<RoomTypeImageEntity> findByRoomType_IdOrderByIsCoverDescCreatedAtAsc(UUID roomTypeId);
}
