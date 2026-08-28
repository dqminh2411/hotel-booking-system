package com.hotelbooking.bookingservice.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hotelbooking.bookingservice.entity.RoomTypeInventory;
import com.hotelbooking.bookingservice.entity.RoomtypeInventoryId;

public interface RoomTypeInventoryRepository extends JpaRepository<RoomTypeInventory, RoomtypeInventoryId> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE roomtype_inventory ri
            SET available_quantity = ri.available_quantity - req.quantity
            FROM jsonb_to_recordset(CAST(:roomRequests AS jsonb))
                AS req(room_type_id uuid, quantity integer)
            WHERE ri.room_type_id = req.room_type_id
              AND ri.inventory_date >= :checkinDate
              AND ri.inventory_date < :checkoutDate
              AND ri.available_quantity >= req.quantity
            """, nativeQuery = true)
    int reserveInventory(@Param("roomRequests") String roomRequestsJson,
                          @Param("checkinDate") LocalDate checkinDate,
                          @Param("checkoutDate") LocalDate checkoutDate);

    List<RoomTypeInventory> findAllByIdInventoryDateBetween(LocalDate from, LocalDate toInclusive);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            INSERT INTO roomtype_inventory (hotel_id, room_type_id, inventory_date, total_quantity, available_quantity)
            SELECT hotel_id, room_type_id, CURRENT_DATE + INTERVAL '179 days', total_quantity, total_quantity
            FROM roomtype_inventory
            WHERE inventory_date = CURRENT_DATE + INTERVAL '178 days'
            ON CONFLICT (room_type_id, inventory_date) DO NOTHING
            """, nativeQuery = true)
    int extendInventoryWindow();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM roomtype_inventory WHERE inventory_date < CURRENT_DATE", nativeQuery = true)
    int deleteExpiredInventory();
}