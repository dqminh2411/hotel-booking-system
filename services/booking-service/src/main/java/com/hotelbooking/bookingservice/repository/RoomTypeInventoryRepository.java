package com.hotelbooking.bookingservice.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hotelbooking.bookingservice.entity.RoomTypeInventory;

import jakarta.persistence.LockModeType;

public interface RoomTypeInventoryRepository extends JpaRepository<RoomTypeInventory, UUID>{

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("Select rti from RoomTypeInventory rti where rti.roomTypeId = :roomTypeId")
    Optional<RoomTypeInventory> findByIdForTruth(@Param("roomTypeId") UUID roomTypeId);
}
