package com.promotion.promotion_service.repository;

import com.promotion.promotion_service.constant.promotion_usage.PromotionUsageStatus;
import com.promotion.promotion_service.entity.PromotionUsageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PromotionUsageRepository extends JpaRepository<PromotionUsageEntity, UUID> {

    Optional<PromotionUsageEntity> findByBookingIdAndStatus(UUID bookingId, PromotionUsageStatus status);

    Optional<PromotionUsageEntity> findByBookingId(UUID bookingId);

    boolean existsByIdempotencyKey(String idempotencyKey);

    long countByUserIdAndPromotionIdAndStatusIn(UUID userId, UUID promotionId, List<PromotionUsageStatus> statuses);

    long countByPromotionIdAndStatusIn(UUID promotionId, List<PromotionUsageStatus> statuses);

    long countByCouponIdAndStatusIn(UUID couponId, List<PromotionUsageStatus> statuses);
}
