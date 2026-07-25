package com.place_booking_service.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Table(indexes = {
    @Index(name = "idx_saga_dup_check", columnList = "userId, hashRequest, createdAt, status")
})
public class SagaState {

    @Id
    private UUID id;

    private UUID bookingId;

    private String idempotencyKey;

    private String status;
    private String currentStep;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /*theo yêu cầu check db khi redis lỗi */
    private String hashRequest;
    private UUID userId;

    private String couponCode;

    @jakarta.persistence.Column(columnDefinition = "TEXT")
    private String pendingPayload;

}
