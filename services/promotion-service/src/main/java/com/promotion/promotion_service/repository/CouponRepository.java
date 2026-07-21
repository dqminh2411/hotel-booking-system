package com.promotion.promotion_service.repository;

import com.promotion.promotion_service.entity.CouponEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CouponRepository extends JpaRepository<CouponEntity, UUID> {

    /**
     * Lấy toàn bộ coupon của một promotion (chưa bị soft delete).
     */
    List<CouponEntity> findAllByPromotionIdAndIsDeletedFalse(UUID promotionId);

    /**
     * Tìm coupon theo id và promotion.
     * Dùng khi update theo cơ chế merge.
     */
    Optional<CouponEntity> findByIdAndPromotionIdAndIsDeletedFalse(
            UUID id,
            UUID promotionId
    );

    /**
     * Kiểm tra coupon code đã tồn tại hay chưa.
     */
    boolean existsByCodeAndIsDeletedFalse(String code);

    /**
     * Kiểm tra trùng code khi update.
     */
    boolean existsByCodeAndIdNotAndIsDeletedFalse(
            String code,
            UUID id
    );

    /**
     * Lấy coupon theo code.
     * Có thể dùng cho validate sau này.
     */
    Optional<CouponEntity> findByCodeAndIsDeletedFalse(String code);

}