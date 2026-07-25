package com.promotion.promotion_service.dto.request;

import com.promotion.promotion_service.constant.scope.ScopeType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionScopeInput {

    @NotNull
    private ScopeType scopeType;

    /**
     * Required for HOTEL, ROOM_TYPE and USER_SEGMENT.
     * Must be null for SYSTEM.
     *
     * Business validation is handled in Service.
     */
    private UUID scopeRefId;

}