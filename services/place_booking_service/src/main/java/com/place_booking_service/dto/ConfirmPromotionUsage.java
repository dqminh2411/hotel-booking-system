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
public class ConfirmPromotionUsage {
    @Builder.Default
    private String eventType = "ConfirmPromotionUsage";
    private UUID sagaId;
    private UUID bookingId;
    private String couponCode;
    private UUID userId;
}
