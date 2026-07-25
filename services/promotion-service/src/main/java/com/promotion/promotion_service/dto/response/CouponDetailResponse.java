package com.promotion.promotion_service.dto.response;

import com.promotion.promotion_service.constant.coupons.CouponStatus;
import com.promotion.promotion_service.constant.promotions.PromotionDiscountType;
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
public class CouponDetailResponse {
    private UUID couponId;
    private String code;
    private CouponStatus status;
    private Integer usageLimit;
    private Integer currentUsageCount;
    private Integer remainingUsages;

    private UUID promotionId;
    private String promotionName;
    private String promotionDescription;
    private PromotionDiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;

    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
}
