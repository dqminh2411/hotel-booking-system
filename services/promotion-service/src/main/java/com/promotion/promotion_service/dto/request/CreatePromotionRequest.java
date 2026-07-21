package com.promotion.promotion_service.dto.request;


import com.promotion.promotion_service.constant.promotions.PromotionDiscountType;
import com.promotion.promotion_service.constant.promotions.PromotionStatus;
import com.promotion.promotion_service.constant.promotions.PromotionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class CreatePromotionRequest {

    /**
     * Ignored in current sprint.
     */
    private UUID tenantId;

    @NotBlank
    @Size(min = 1, max = 150)
    private String name;

    @Size(max = 2000)
    private String description;

    @NotNull
    private PromotionType type;

    @NotNull
    private PromotionDiscountType discountType;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = true)
    private BigDecimal discountValue;

    @DecimalMin(value = "0.00", inclusive = true)
    private BigDecimal maxDiscountAmount;

    @DecimalMin(value = "0.00", inclusive = true)
    private BigDecimal minBookingAmount;

    @Min(1)
    private Integer minNights;

    @NotNull
    private OffsetDateTime startAt;

    @NotNull
    private OffsetDateTime endAt;

    @NotNull
    private PromotionStatus status;

    @Min(1)
    private Integer totalUsageLimit;

    @Min(1)
    private Integer perUserUsageLimit;

    @Builder.Default
    private Boolean stackable = false;

    @Valid
    @NotEmpty
    private List<PromotionScopeInput> scopes;

    @Valid
    @Builder.Default
    private List<CouponInput> coupons = List.of();

    @Valid
    @Builder.Default
    private List<PromotionConditionInput> conditions = List.of();

}