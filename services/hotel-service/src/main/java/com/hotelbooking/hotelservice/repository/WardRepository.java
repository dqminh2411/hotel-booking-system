package com.hotelbooking.hotelservice.repository;

import com.hotelbooking.hotelservice.entity.WardEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WardRepository extends JpaRepository<WardEntity, String> {
}
