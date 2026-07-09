package com.hotelbooking.hotelservice.repository;

import com.hotelbooking.hotelservice.entity.AmenityEntity;
import com.hotelbooking.hotelservice.entity.HotelAmenityEntity;
import com.hotelbooking.hotelservice.entity.HotelAmenityId;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HotelAmenityRepository extends JpaRepository<HotelAmenityEntity, HotelAmenityId> {

    @Query("""
            SELECT a FROM HotelAmenityEntity ha
            JOIN ha.amenity a
            WHERE ha.hotel.id = :hotelId
              AND ha.isDeleted = false
              AND a.isDeleted = false
            """)
    List<AmenityEntity> findActiveAmenitiesByHotelId(@Param("hotelId") UUID hotelId);
}