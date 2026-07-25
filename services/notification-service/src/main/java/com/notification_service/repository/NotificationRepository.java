package com.notification_service.repository;

import com.notification_service.entity.NotificationEntity;
import com.notification_service.enums.NotificationEventType;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    @Query("""
        select n from NotificationEntity n
        where n.recipientUserId = :recipientUserId
          and (:unreadOnly = false or n.readAt is null)
          and (:eventType is null or n.eventType = :eventType)
        """)
    Page<NotificationEntity> findInbox(
        @Param("recipientUserId") UUID recipientUserId,
        @Param("unreadOnly") boolean unreadOnly,
        @Param("eventType") NotificationEventType eventType,
        Pageable pageable
    );

    long countByRecipientUserIdAndReadAtIsNull(UUID recipientUserId);
}
