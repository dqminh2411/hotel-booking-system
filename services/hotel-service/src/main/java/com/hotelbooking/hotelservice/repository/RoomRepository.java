package com.hotelbooking.hotelservice.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hotelbooking.hotelservice.constant.RoomStatus;
import com.hotelbooking.hotelservice.entity.RoomEntity;

public interface RoomRepository extends JpaRepository<RoomEntity, UUID>{

    List<RoomEntity> findByRoomType_IdInAndStatus(List<UUID> roomTypeIds, RoomStatus status);

    @Modifying
    @Query("UPDATE RoomEntity r SET r.status = :newStatus WHERE r.id IN :roomIds AND r.status = :oldStatus")
    int updateStatusRooms(
            @Param("roomIds") List<UUID> roomIds, 
            @Param("oldStatus") RoomStatus oldStatus, 
            @Param("newStatus") RoomStatus newStatus
    );
}
