package com.hotelbooking.bookingservice.repository;

import com.hotelbooking.bookingservice.entity.BookingInfoEntity;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingInfoRepository extends JpaRepository<BookingInfoEntity, UUID> {
}
