package com.hotelbooking.bookingservice.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Embeddable
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
public class RoomtypeInventoryId implements Serializable{

    @Column(name = "room_type_id", nullable = false)
    UUID roomTypeId;

    @Column(name = "inventory_date", nullable = false)
    LocalDate inventoryDate;
}
