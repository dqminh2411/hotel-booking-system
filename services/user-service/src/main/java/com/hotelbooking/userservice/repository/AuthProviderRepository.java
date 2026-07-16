package com.hotelbooking.userservice.repository;

import com.hotelbooking.userservice.entity.AuthProviderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthProviderRepository extends JpaRepository<AuthProviderEntity, String> {
    Optional<AuthProviderEntity> findByCode(String code);
}
