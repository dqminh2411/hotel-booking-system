package com.hotelbooking.hotelservice.repository;

import com.hotelbooking.hotelservice.entity.RoomTypeEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomTypeRepository extends JpaRepository<RoomTypeEntity, String> {

    List<RoomTypeEntity> findByHotel_Id(String hotelId);

    Optional<RoomTypeEntity> findById(String roomTypeId);

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


