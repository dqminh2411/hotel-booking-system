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

    static List<UUID> findByHOTELScope(List<AmenityEntity> amenities) {
        return List.of();
    }

    @Query("""
        SELECT hae.hotel.id
        FROM HotelAmenityEntity hae
        WHERE hae.hotel.id IN :hotelIds
            AND hae.amenity.id IN :amenityIds
            AND hae.isDeleted = false
            AND hae.amenity.isDeleted = false
        GROUP BY hae.hotel.id
        HAVING COUNT(DISTINCT hae.amenity.id) = :amenityCount
        
""")
    List<UUID> findHotelIdsByAmenityIds(
            @Param("hotelIds") List<UUID> hotelIds,
            @Param("amenityIds") List<UUID> amenityIds,
            @Param("amenityCount") long amenityCount
    );

    @Query("""
            SELECT a FROM HotelAmenityEntity ha
            JOIN ha.amenity a
            WHERE ha.hotel.id = :hotelId
              AND ha.isDeleted = false
              AND a.isDeleted = false
            """)
    List<AmenityEntity> findActiveAmenitiesByHotelId(@Param("hotelId") UUID hotelId);
}