package com.place_booking_service.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
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

}
