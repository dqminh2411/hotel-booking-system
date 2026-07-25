package com.promotion.promotion_service.dto.event;

import com.promotion.promotion_service.constant.promotions.PromotionDiscountType;
import com.promotion.promotion_service.constant.promotions.PromotionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionActiveNotificationEvent {

    private UUID eventId;
    private String eventType; // PromotionActiveCreated or PromotionActiveUpdated
    private UUID promotionId;
    private String name;
    private String description;
    private PromotionType type;
    private PromotionDiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private OffsetDateTime startAt;
    private OffsetDateTime endAt;
    private String scopeType;
    private UUID scopeRefId;
    private OffsetDateTime occurredAt;
}
