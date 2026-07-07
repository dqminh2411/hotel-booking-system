package com.hotelbooking.hotelservice.repository;

import com.hotelbooking.hotelservice.entity.HotelEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HotelRepository extends JpaRepository<HotelEntity, String> {

    Page<HotelEntity> findByNameContainingIgnoreCaseAndAddressContainingIgnoreCase(String name, String address, Pageable pageable);


    @Query(value = """
        SELECT *
        FROM hotels
        WHERE province_code = :provinceCode
          AND status = 'APPROVED'
          AND is_deleted = FALSE
        """, nativeQuery = true)
    List<HotelEntity> findByProvinceCode(@Param("provinceCode") String provinceCode);

    @Query(value = """
        SELECT *
        FROM hotels
        WHERE district_code = :districtCode
            AND status = 'APPROVED'
            AND is_deleted = FALSE
    """, nativeQuery = true)
    List<HotelEntity> findByDistrictCode(@Param("districtCode") String districtCode);

    @Query(value = """
        SELECT *
        FROM hotels
        WHERE ward_code = :wardCode
            AND status = 'APPROVED'
            AND is_deleted = FALSE
    """, nativeQuery = true)
    List<HotelEntity> findByWardCode(@Param("wardCode") String wardCode);
}


