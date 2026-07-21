package com.promotion.promotion_service.dto.response;

import com.promotion.promotion_service.constant.condition.ConditionType;
import com.promotion.promotion_service.constant.condition.ConditionOperator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionConditionResponse {

    private UUID id;

    private ConditionType conditionType;

    private ConditionOperator operator;

    private String conditionValue;

}