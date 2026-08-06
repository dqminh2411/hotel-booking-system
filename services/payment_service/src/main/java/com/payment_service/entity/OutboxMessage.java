package com.payment_service.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class OutboxMessage {

    @Id
    private UUID id;

    private String eventType;
    private String topic;

    @Column(columnDefinition = "TEXT")
    private String payload;

    private String status;
    private int retryCount;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;

    /**
     * W3C Trace Context header — được capture khi HTTP request gọi saveOutboxMessage().
     * Dùng để restore OTel trace context khi scheduled relay publish lên Kafka,
     * đảm bảo span producer thuộc cùng trace với HTTP request gốc.
     * Format: "00-<traceId>-<spanId>-01"
     */
    @Column(name = "traceparent", length = 55)
    private String traceparent;
}
