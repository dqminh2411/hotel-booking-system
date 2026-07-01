package com.hotelbooking.userservice.repository;

import com.hotelbooking.userservice.entity.RoleEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<RoleEntity, UUID> {
    Optional<RoleEntity> findByNameAndDeletedFalse(String name);
}
