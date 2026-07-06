package com.notification_service.repository;

import com.notification_service.entity.NotificationDeliveryLogEntity;
import com.notification_service.enums.DeliveryStatus;
import com.notification_service.enums.NotificationChannel;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationDeliveryLogRepository
    extends JpaRepository<NotificationDeliveryLogEntity, UUID> {

    List<NotificationDeliveryLogEntity> findByNotificationId(UUID notificationId);

    @Query("""
        select log from NotificationDeliveryLogEntity log
        where log.status = :status
          and log.retryCount < log.maxRetries
        """)
    List<NotificationDeliveryLogEntity> findRetryableByStatus(
        @Param("status") DeliveryStatus status
    );

    List<NotificationDeliveryLogEntity> findByChannelAndStatus(
        NotificationChannel channel,
        DeliveryStatus status
    );
}
