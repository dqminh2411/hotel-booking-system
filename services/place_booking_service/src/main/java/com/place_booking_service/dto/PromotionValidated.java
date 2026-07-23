package com.place_booking_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionValidated {
    private String eventType;
    private UUID sagaId;
    private UUID bookingId;
    private UUID promotionId;
    private UUID couponId;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
}
