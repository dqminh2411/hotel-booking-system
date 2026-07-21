package com.promotion.promotion_service.dto.request;

import com.promotion.promotion_service.constant.condition.ConditionOperator;
import com.promotion.promotion_service.constant.condition.ConditionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionConditionInput {

    @NotNull
    private ConditionType conditionType;

    @NotNull
    private ConditionOperator operator;

    @NotBlank
    @Size(min = 1, max = 100)
    private String conditionValue;

}