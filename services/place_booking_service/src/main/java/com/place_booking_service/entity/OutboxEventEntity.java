package com.place_booking_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.JdbcTypeCode;

@Getter
@Setter
@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "topic", nullable = false)
    private String topic;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    /**
     * W3C Trace Context header — được capture khi HTTP request gọi saveOutboxMessage().
     * Dùng để restore OTel trace context khi scheduled relay publish lên Kafka,
     * đảm bảo span producer thuộc cùng trace với HTTP request gốc.
     * Format: "00-<traceId>-<spanId>-01"
     */
    @Column(name = "traceparent", length = 55)
    private String traceparent;
}
