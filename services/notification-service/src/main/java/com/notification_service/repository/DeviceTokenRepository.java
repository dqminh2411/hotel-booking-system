package com.notification_service.repository;

import com.notification_service.entity.DeviceTokenEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceTokenRepository extends JpaRepository<DeviceTokenEntity, UUID> {

    Optional<DeviceTokenEntity> findByFcmToken(String fcmToken);

    Optional<DeviceTokenEntity> findByFcmTokenAndActiveTrue(String fcmToken);

    Optional<DeviceTokenEntity> findFirstByFcmTokenAndUserIdOrderByUpdatedAtDesc(String fcmToken, UUID userId);

    Optional<DeviceTokenEntity> findByUserIdAndFcmTokenAndActiveTrue(UUID userId, String fcmToken);

    List<DeviceTokenEntity> findByUserIdAndActiveTrue(UUID userId);
}
