package com.hotelbooking.hotelservice.repository;

import com.hotelbooking.hotelservice.entity.HotelImageEntity;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelImageRepository extends JpaRepository<HotelImageEntity, UUID> {

    List<HotelImageEntity> findByHotel_IdOrderByIsCoverDescCreatedAtAsc(UUID hotelId);
}