package com.promotion.promotion_service.dto.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionUsageConfirmedEvent {
    @Builder.Default
    private String eventType = "PromotionUsageConfirmed";
    private UUID sagaId;
    private UUID bookingId;
}
