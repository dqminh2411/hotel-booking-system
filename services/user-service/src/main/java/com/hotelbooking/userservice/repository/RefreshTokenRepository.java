package com.hotelbooking.userservice.repository;

import com.hotelbooking.userservice.entity.RefreshTokenEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {
    @EntityGraph(attributePaths = {"user", "user.roles"})
    Optional<RefreshTokenEntity> findByTokenHashAndRevokedFalse(String tokenHash);
}
