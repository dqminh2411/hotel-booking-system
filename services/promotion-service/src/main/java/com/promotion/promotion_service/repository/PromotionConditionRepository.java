package com.promotion.promotion_service.repository;

import com.promotion.promotion_service.constant.condition.ConditionType;
import com.promotion.promotion_service.entity.PromotionConditionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PromotionConditionRepository
        extends JpaRepository<PromotionConditionEntity, UUID> {

    /**
     * Lấy tất cả điều kiện của một promotion.
     */
    List<PromotionConditionEntity> findAllByPromotionId(UUID promotionId);

    /**
     * Xóa toàn bộ điều kiện của một promotion.
     */
    void deleteAllByPromotionId(UUID promotionId);

    /**
     * Lấy condition theo loại.
     */
    List<PromotionConditionEntity> findAllByPromotionIdAndConditionType(
            UUID promotionId,
            ConditionType conditionType
    );

}