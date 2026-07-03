package com.hotelbooking.hotelservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import com.hotelbooking.hotelservice.constant.HotelStatus;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "hotels")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
public class HotelEntity {

    @Id
    @Column(name = "id", nullable = false, length = 255)
    @EqualsAndHashCode.Include
    @ToString.Include
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "tenant_id", nullable = false)
    UUID tenantId;

    @Column(name = "name", nullable = false)
    @ToString.Include
    String name;

    @Column(name = "description")
    String description;

    @Column(name = "address", nullable = false)
    @ToString.Include
    String address;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "province_code", nullable = false)
    ProvinceEntity province;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "district_code", nullable = false)
    DistrictEntity district;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ward_code", nullable = false)
    WardEntity ward;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    HotelStatus status = HotelStatus.PENDING;

    @Column(name = "is_deleted", nullable = false)
    Boolean isDeleted = false;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    @OneToMany(mappedBy = "hotel")
    @BatchSize(size = 30)
    List<RoomTypeEntity> roomTypes = new ArrayList<>();

    @OneToMany(mappedBy = "hotel")
    @BatchSize(size = 30)
    List<HotelImageEntity> hotelImages = new ArrayList<>();

    @OneToMany(mappedBy = "hotel")
    @BatchSize(size = 30)
    List<PolicyEntity> policies = new ArrayList<>();

    @OneToMany(mappedBy = "hotel")
    @BatchSize(size = 30)
    List<HotelAmenityEntity> hotelAmenities = new ArrayList<>();
}

