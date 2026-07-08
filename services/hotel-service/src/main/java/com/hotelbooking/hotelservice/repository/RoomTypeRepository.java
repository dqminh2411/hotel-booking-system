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

    List<RoomTypeEntity> findByHotel_IdAndIdIn(UUID hotelId, List<UUID> roomTypeList);

    @Query("""
            SELECT DISTINCT rt FROM RoomTypeEntity rt
            LEFT JOIN FETCH rt.roomTypeImages img
            WHERE rt.hotel.id = :hotelId
              AND rt.isDeleted = false
              AND (img.isCover = true OR img IS NULL)
            """)
    List<RoomTypeEntity> findActiveByHotelIdWithCoverImage(@Param("hotelId") UUID hotelId);
}


