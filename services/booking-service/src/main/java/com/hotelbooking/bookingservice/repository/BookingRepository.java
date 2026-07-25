package com.hotelbooking.bookingservice.repository;

import com.hotelbooking.bookingservice.dto.ActiveBookingRoomType;
import com.hotelbooking.bookingservice.entity.BookingEntity;
import com.hotelbooking.bookingservice.enums.BookingStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<BookingEntity, UUID> {
    @Query("""
        select b from BookingEntity b
        where b.id = :bookingId
    """)
    Optional<BookingEntity> findByBookingId(@Param("bookingId") UUID bookingId);

    @Query("""
        select new com.hotelbooking.bookingservice.dto.ActiveBookingRoomType(br.roomTypeId, coalesce(sum(br.quantity), 0))
        from BookedRoomTypeEntity br, BookingEntity b
        where br.bookingId = b.id
          and (:hotelId is null or b.hotelId = :hotelId)
          and (:roomTypeIdsEmpty = true or br.roomTypeId in (:roomTypeIds))
          and b.status in (:activeStatuses)
          and b.checkinDate < :checkout
          and b.checkoutDate > :checkin
        group by br.roomTypeId
        """)
    List<ActiveBookingRoomType> countActiveBookingsByRoomType(
        @Param("hotelId") UUID hotelId,
        @Param("roomTypeIds") Collection<UUID> roomTypeIds,
        @Param("roomTypeIdsEmpty") boolean roomTypeIdsEmpty,
        @Param("checkin") LocalDate checkin,
        @Param("checkout") LocalDate checkout,
        @Param("activeStatuses") Collection<BookingStatus> activeStatuses
    );

    @Query("select be from BookingEntity be where be.hotelId = :hotelId " +
            "and ((cast(:today AS date)) is null or be.checkinDate = :today) " + 
            "and be.status = :status"
    )
    Page<BookingEntity> findByHotelIdAndCheckinDateAndStatus(@Param("hotelId") UUID hotelId,
        @Param("today") LocalDate today, 
        @Param("status") BookingStatus status, Pageable pageable);
}
