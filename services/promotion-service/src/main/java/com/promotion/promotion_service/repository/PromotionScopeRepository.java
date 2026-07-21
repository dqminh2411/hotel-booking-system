package com.promotion.promotion_service.repository;

import com.promotion.promotion_service.constant.scope.ScopeType;
import com.promotion.promotion_service.entity.PromotionScopeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PromotionScopeRepository
        extends JpaRepository<PromotionScopeEntity, UUID> {

    /**
     * Lấy toàn bộ scope của promotion.
     */
    List<PromotionScopeEntity> findAllByPromotionId(UUID promotionId);

    /**
     * Xóa toàn bộ scope của promotion.
     */
    void deleteAllByPromotionId(UUID promotionId);

    /**
     * Lấy scope theo loại.
     */
    List<PromotionScopeEntity> findAllByPromotionIdAndScopeType(
            UUID promotionId,
            ScopeType scopeType
    );

}