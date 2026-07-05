package com.hotelbooking.hotelservice.repository;

import com.hotelbooking.hotelservice.constant.HotelStatus;
import com.hotelbooking.hotelservice.entity.HotelEntity;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelRepository extends JpaRepository<HotelEntity, UUID> {

    Page<HotelEntity> findByNameContainingIgnoreCaseAndAddressContainingIgnoreCase(String name, String address,
            Pageable pageable);

    @EntityGraph(attributePaths = {"province", "district", "ward"})
    Optional<HotelEntity> findByIdAndIsDeletedFalseAndStatus(UUID id, HotelStatus status);
}
