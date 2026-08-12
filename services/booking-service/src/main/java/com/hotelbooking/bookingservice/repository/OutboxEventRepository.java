package com.hotelbooking.bookingservice.repository;

import com.hotelbooking.bookingservice.entity.OutboxEventEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {

    @Query(value="""
            SELECT * FROM outbox_events 
            WHERE status = 'PENDING'
            OR (status = 'PROCESSING' AND locked_until < NOW() AND retry_count < 5 AND (next_retry_at IS NULL OR next_retry_at <= NOW() ))
            ORDER BY created_at ASC
            LIMIT 100
            FOR UPDATE SKIP LOCKED;
            """, nativeQuery = true)
    List<OutboxEventEntity> findEventsToPublish();

}
