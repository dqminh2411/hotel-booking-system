package com.hotelbooking.bookingservice.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Entity
@Table(name = "roomtype_inventory")
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomTypeInventory {

    @EmbeddedId
    RoomtypeInventoryId id;
    
    @Column(name = "hotel_id", nullable = false)
    UUID hotelId;

    @Column(name = "total_quantity", nullable = false)
    Integer totalQuantity;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;
}
