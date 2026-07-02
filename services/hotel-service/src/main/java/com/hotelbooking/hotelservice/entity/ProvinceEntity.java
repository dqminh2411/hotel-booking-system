package com.hotelbooking.hotelservice.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
@Table(name = "provinces")
public class ProvinceEntity {

    @Id
    @Column(name = "code", length = 2)
    String code;

    @Column(name = "name", nullable = false, length = 100)
    String name;
    
    @Column(name = "name_en", nullable = true, length = 100)
    String name_en;

    @Column(name = "full_name", nullable = true, length = 255)
    String fullname;

    @Column(name = "is_deleted", nullable = false)
    Boolean isDeleted = false;

    @OneToMany(mappedBy = "province")
    List<DistrictEntity> districts = new ArrayList<>();

    @OneToMany(mappedBy = "province")
    List<HotelEntity> hotels = new ArrayList<>();
}
