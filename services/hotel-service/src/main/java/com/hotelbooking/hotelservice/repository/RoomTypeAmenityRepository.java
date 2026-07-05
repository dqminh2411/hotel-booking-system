package com.hotelbooking.hotelservice.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hotelbooking.hotelservice.entity.AmenityEntity;
import com.hotelbooking.hotelservice.entity.RoomTypeAmenityEntity;
import com.hotelbooking.hotelservice.entity.RoomTypeAmenityId;

public interface RoomTypeAmenityRepository extends JpaRepository<RoomTypeAmenityEntity, RoomTypeAmenityId>{

    @Query("""
            SELECT a FROM RoomTypeAmenityEntity rta
            JOIN rta.amenity a
            WHERE rta.roomType.id = :roomTypeId
              AND rta.isDeleted = false
              AND a.isDeleted = false
            """)
    List<AmenityEntity> findActiveAmenitiesByRoomTypeId(@Param("roomTypeId") UUID roomTypeId);
}
