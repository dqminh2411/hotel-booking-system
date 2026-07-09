package com.hotelbooking.hotelservice.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "wards")
public class WardEntity {

    @Id
    @Column(name = "code", length = 5)
    String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "district_code", nullable = false)
    DistrictEntity district;

    @Column(name = "name", nullable = false, length = 100)
    String name;

    @Column(name = "name_en", nullable = true, length = 100)
    String name_en;

    @Column(name = "full_name", nullable = true, length = 255)
    String fullname;

    @Column(name = "is_deleted", nullable = false)
    Boolean isDeleted = false;

    @OneToMany(mappedBy = "ward")
    List<HotelEntity> hotels = new ArrayList<>();
}
