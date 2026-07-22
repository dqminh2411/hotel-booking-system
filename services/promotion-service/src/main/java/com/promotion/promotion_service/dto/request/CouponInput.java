package com.promotion.promotion_service.dto.request;

import com.promotion.promotion_service.constant.coupons.CouponStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponInput {

    @NotBlank
    @Size(min = 1, max = 50)
    @Pattern(
            regexp = "^[A-Z0-9_-]+$",
            message = "Coupon code must contain only uppercase letters, numbers, underscore and hyphen"
    )
    private String code;

    @NotNull
    private CouponStatus status;

    @Min(1)
    private Integer usageLimit;

}