package com.hotelbooking.hotelservice.repository;

import com.hotelbooking.hotelservice.entity.HotelImageEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelImageRepository extends JpaRepository<HotelImageEntity, UUID> {

    List<HotelImageEntity> findByHotel_IdOrderByIsCoverDescCreatedAtAsc(UUID hotelId);
    List<HotelImageEntity> findByHotel_IdInAndIsCoverTrue(List<UUID> hotelIds);

    List<HotelImageEntity> findAllByIdInAndHotel_Id(List<UUID> imgIds, UUID hotelId);

    // Long them IsDeletedFalse
    List<HotelImageEntity> findByHotel_IdAndIsDeletedFalseOrderByIsCoverDescCreatedAtAsc(UUID hotelId);

    List<HotelImageEntity> findByHotel_IdInAndIsCoverTrueAndIsDeletedFalse(List<UUID> hotelIds);

    List<HotelImageEntity> findAllByIdInAndHotel_IdAndIsDeletedFalse(List<UUID> imgIds, UUID hotelId);

    Optional<HotelImageEntity> findByHotel_IdAndIsCoverTrueAndIsDeletedFalse(UUID hotelId);
}