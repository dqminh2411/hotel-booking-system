package com.hotelbooking.hotelservice.entity;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomTypeAmenityId implements Serializable {

    @Column(name = "room_type_id")
    UUID roomTypeId;

    @Column(name = "amenity_id")
    UUID amenityId;
}
