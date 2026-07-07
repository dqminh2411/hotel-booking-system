package com.hotelbooking.hotelservice.entity;

import com.hotelbooking.hotelservice.enums.HotelStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "hotels")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class HotelEntity {

    @Id
    @Column(name = "id", nullable = false, length = 255)
    @EqualsAndHashCode.Include
    @ToString.Include
    private String id;

    @Column(name = "name", nullable = false)
    @ToString.Include
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "host_id", nullable = false, length = 255)
    private String hostId;

    @Column(name = "tenant_id", nullable = false, length = 255)
    private String tenant_id;

    @Column(name = "address", nullable = false)
    @ToString.Include
    private String address;

    @Column(name = "province_code", nullable = false, length = 2)
    private String provinceCode;

    @Column(name = "district_code", nullable = false, length = 3)
    private  String districtCode;

    @Column(name = "ward_code", nullable = false, length = 5)
    private String wardCode;

    @Enumerated(EnumType.STRING)
    @Column (name = "status", nullable = false)
    private HotelStatus status;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "hotel")
    private List<com.hotelbooking.hotelservice.entity.RoomTypeEntity> roomTypes = new ArrayList<>();

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

