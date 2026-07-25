package com.promotion.promotion_service.dto.response;

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
public class ValidatePromotionPreResponse {
    private UUID promotionId;
    private UUID couponId;
    private String couponCode;
    private String promotionName;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String discountDescription;
}
