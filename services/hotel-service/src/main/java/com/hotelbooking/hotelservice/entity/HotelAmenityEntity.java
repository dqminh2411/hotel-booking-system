package com.hotelbooking.hotelservice.entity;


import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Entity
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "hotel_amenities")
public class HotelAmenityEntity {

    @EmbeddedId
    HotelAmenityId id;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("hotelId")
    @JoinColumn(name = "hotel_id", nullable = false)
    HotelEntity hotel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("amenityId")
    @JoinColumn(name = "amenity_id", nullable = false)
    AmenityEntity amenity;

    @Column(name = "is_deleted", nullable = false)
    Boolean isDeleted = false;
}
