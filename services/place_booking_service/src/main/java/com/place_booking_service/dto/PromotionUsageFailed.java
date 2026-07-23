package com.place_booking_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionUsageFailed {
    private String eventType;
    private UUID sagaId;
    private UUID bookingId;
    private String reason;
}
