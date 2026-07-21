package com.promotion.promotion_service.dto.response;

import com.promotion.promotion_service.constant.coupons.CouponStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponResponse {

    private UUID id;

    private String code;

    private CouponStatus status;

    private Integer usageLimit;

    private Integer currentUsageCount;

}