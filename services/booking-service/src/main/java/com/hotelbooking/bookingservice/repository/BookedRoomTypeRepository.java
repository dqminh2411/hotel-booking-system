package com.hotelbooking.bookingservice.repository;

import com.hotelbooking.bookingservice.entity.BookedRoomTypeEntity;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BookedRoomTypeRepository extends JpaRepository<BookedRoomTypeEntity, UUID> {
    List<BookedRoomTypeEntity> findByBookingId(UUID bookingId);
}
