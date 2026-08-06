package com.promotion.promotion_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OutboxEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    UUID id;

    @Column(name = "topic", nullable = false, length = 100)
    String topic;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    String payload;

    @Column(name = "published", nullable = false)
    boolean published;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @Column(name = "published_at")
    LocalDateTime publishedAt;

    @Column(name = "is_deleted", nullable = false)
    boolean isDeleted;

    /**
     * W3C Trace Context header — được capture khi HTTP request gọi saveOutboxMessage().
     * Dùng để restore OTel trace context khi scheduled relay publish lên Kafka,
     * đảm bảo span producer thuộc cùng trace với HTTP request gốc.
     * Format: "00-<traceId>-<spanId>-01"
     */
    @Column(name = "traceparent", length = 55)
    String traceparent;
}
