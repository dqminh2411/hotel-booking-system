package com.hotelbooking.hotelservice.repository;

import com.hotelbooking.hotelservice.entity.PolicyEntity;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyRepository extends JpaRepository<PolicyEntity, UUID> {

    List<PolicyEntity> findByHotel_IdAndIsDeletedFalse(UUID hotelId);
}