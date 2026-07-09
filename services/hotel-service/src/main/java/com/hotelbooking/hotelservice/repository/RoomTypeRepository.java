package com.hotelbooking.hotelservice.repository;

import com.hotelbooking.hotelservice.entity.RoomTypeEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomTypeRepository extends JpaRepository<RoomTypeEntity, UUID> {

    List<RoomTypeEntity> findByHotel_Id(UUID hotelId);

    Optional<RoomTypeEntity> findByIdAndIsDeletedFalse(UUID roomTypeId);

    List<RoomTypeEntity> findByHotel_IdAndIdIn(UUID hotelId, List<String> roomTypeList);

    @Query("""
            SELECT DISTINCT rt FROM RoomTypeEntity rt
            LEFT JOIN FETCH rt.roomTypeImages img
            WHERE rt.hotel.id = :hotelId
              AND rt.isDeleted = false
              AND (img.isCover = true OR img IS NULL)
            """)
    List<RoomTypeEntity> findActiveByHotelIdWithCoverImage(@Param("hotelId") UUID hotelId);
    List<RoomTypeEntity> findByHotel_IdAndIdIn(String hotelId, List<String> roomTypeList);

    @Query(value = """
        SELECT * 
        FROM room_types 
        WHERE hotel_id IN (:hotelIds)
          AND max_guests >= :guestNum
          AND is_deleted = false 
        ORDER BY hotel_id ASC,
                 base_price_per_night ASC
""", nativeQuery = true)
    List<RoomTypeEntity> findByHotelIdsAndGuestNum(@Param("hotelIds") List<String> hotelIds,
                                                   @Param("guestNum") int guestNum);
}


