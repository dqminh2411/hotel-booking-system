package com.hotelbooking.userservice.repository;

import com.hotelbooking.userservice.entity.AuthProviderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthProviderRepository extends JpaRepository<AuthProviderEntity, String> {
    Optional<AuthProviderEntity> findByCode(String code);
}
