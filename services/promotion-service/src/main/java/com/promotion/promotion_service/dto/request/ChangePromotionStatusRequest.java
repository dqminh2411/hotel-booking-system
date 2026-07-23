package com.promotion.promotion_service.dto.request;

import com.promotion.promotion_service.constant.promotions.PromotionStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePromotionStatusRequest {
    @NotNull
    private PromotionStatus status;

    private String reason;
}
