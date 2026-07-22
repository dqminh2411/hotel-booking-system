package com.promotion.promotion_service.dto.response;


import com.promotion.promotion_service.constant.promotions.PromotionDiscountType;
import com.promotion.promotion_service.constant.promotions.PromotionStatus;
import com.promotion.promotion_service.constant.promotions.PromotionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionResponse {

    private UUID id;

    private UUID tenantId;

    private String name;

    private String description;

    private PromotionType type;

    private PromotionDiscountType discountType;

    private BigDecimal discountValue;

    private BigDecimal maxDiscountAmount;

    private BigDecimal minBookingAmount;

    private Integer minNights;

    private OffsetDateTime startAt;

    private OffsetDateTime endAt;

    private PromotionStatus status;

    private Integer totalUsageLimit;

    private Integer perUserUsageLimit;

    private Integer currentUsageCount;

    private Boolean stackable;

    private Boolean isDeleted;

    private List<PromotionScopeResponse> scopes;

    private List<CouponResponse> coupons;

    private List<PromotionConditionResponse> conditions;

    private UUID createdBy;

    private UUID updatedBy;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    private OffsetDateTime deletedAt;

}