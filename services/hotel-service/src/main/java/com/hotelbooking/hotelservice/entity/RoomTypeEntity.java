package com.hotelbooking.hotelservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "room_types")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomTypeEntity {

    @Id
    @Column(name = "id", nullable = false, length = 255)
    @EqualsAndHashCode.Include
    @ToString.Include
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hotel_id", nullable = false)
    HotelEntity hotel;

    @Column(name = "name", nullable = false, length = 255)
    @ToString.Include
    String name;

    @Column(name = "description")
    String description;

    @Column(name = "max_guests", nullable = false)
    Integer maxGuests;

    @Column(name = "bed_counts", nullable = false)
    Integer bedCounts;

    @Column(name = "base_price_per_night", nullable = false)
    BigDecimal basePricePerNight;

    @Column(name = "quantity", nullable = false)
    Integer quantity;

    @Column(name = "area")
    Integer area;

    @Column(name = "is_deleted", nullable = false)
    Boolean isDeleted = false;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    @OneToMany(mappedBy = "roomType")
    List<RoomEntity> rooms = new ArrayList<>();

    @OneToMany(mappedBy = "roomType")
    List<RoomTypeImageEntity> roomTypeImages = new ArrayList<>();

    @OneToMany(mappedBy = "roomType")
    List<PricingRuleEntity> pricingRules = new ArrayList<>();

    @OneToMany(mappedBy = "roomType")
    List<RoomTypeAmenityEntity> roomTypeAmenities = new ArrayList<>();
}


