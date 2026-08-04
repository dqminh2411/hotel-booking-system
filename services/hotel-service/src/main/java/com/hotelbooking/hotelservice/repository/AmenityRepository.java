package com.hotelbooking.hotelservice.repository;

import com.hotelbooking.hotelservice.entity.AmenityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AmenityRepository extends JpaRepository<AmenityEntity, UUID> {

    Optional<AmenityEntity> findByNameIgnoreCaseAndIsDeletedFalse(String name);

}
