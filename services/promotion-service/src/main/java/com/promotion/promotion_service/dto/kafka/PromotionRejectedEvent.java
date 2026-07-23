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
public class PromotionRejectedEvent {
    @Builder.Default
    private String eventType = "PromotionRejected";
    private UUID sagaId;
    private UUID bookingId;
    private String reason;
}
