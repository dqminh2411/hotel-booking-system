package com.promotion.promotion_service.dto.kafka;

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
public class PromotionValidatedEvent {
    @Builder.Default
    private String eventType = "PromotionValidated";
    private UUID sagaId;
    private UUID bookingId;
    private UUID promotionId;
    private UUID couponId;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
}
