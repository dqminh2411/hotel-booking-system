package com.promotion.promotion_service.dto.event;

import com.promotion.promotion_service.constant.promotions.PromotionDiscountType;

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
public class CouponActiveNotificationEvent {

    private UUID eventId;
    private String eventType; // CouponActiveCreated or CouponActiveUpdated
    private UUID couponId;
    private UUID promotionId;
    private String code;
    private String promotionName;
    private String promotionDescription;
    private PromotionDiscountType discountType;
    private BigDecimal discountValue;
    private Integer usageLimit;
    private OffsetDateTime occurredAt;
}
