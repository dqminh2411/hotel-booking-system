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

    static List<UUID> findByROOM_TYPEScope(List<AmenityEntity> amenities) {
        return List.of();
    }

    @Query("""
        SELECT rta.roomType.id
        FROM RoomTypeAmenityEntity rta
        WHERE rta.roomType.hotel.id IN :hotelIds
            AND rta.amenity.id IN :amenityIds
            AND rta.isDeleted = false
            AND rta.amenity.isDeleted = false
        GROUP BY rta.roomType.id
        HAVING COUNT(DISTINCT rta.amenity.id) = :amenityCount
        
""") List<UUID> findRoomTypeIdsByAmenityIds( // lấy các room_type có tất cả amenities từ request
        @Param("hotelIds") List<UUID> hotelIds,
        @Param("amenityIds") List<UUID> amenityIds,
        @Param("amenityCount") long amenityCount);

    @Query("""
            SELECT a FROM RoomTypeAmenityEntity rta
            JOIN rta.amenity a
            WHERE rta.roomType.id = :roomTypeId
              AND rta.isDeleted = false
              AND a.isDeleted = false
            """)
    List<AmenityEntity> findActiveAmenitiesByRoomTypeId(@Param("roomTypeId") UUID roomTypeId);
}
