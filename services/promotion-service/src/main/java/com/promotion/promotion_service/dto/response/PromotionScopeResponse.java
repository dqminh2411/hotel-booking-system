package com.promotion.promotion_service.dto.response;

import com.promotion.promotion_service.constant.scope.ScopeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionScopeResponse {

    private UUID id;

    private ScopeType scopeType;

    private UUID scopeRefId;

}