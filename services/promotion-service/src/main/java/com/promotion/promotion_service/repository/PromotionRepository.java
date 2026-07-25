package com.promotion.promotion_service.repository;

import com.promotion.promotion_service.constant.promotions.PromotionStatus;
import com.promotion.promotion_service.entity.PromotionEntity;
import feign.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface PromotionRepository extends JpaRepository<PromotionEntity, UUID> {

    Optional<PromotionEntity> findByIdAndIsDeletedFalse(UUID id);

    boolean existsByIdAndIsDeletedFalse(UUID id);

    Page<PromotionEntity> findAllByIsDeletedFalse(Pageable pageable);

    Page<PromotionEntity> findAllByStatusAndIsDeletedFalse(
            PromotionStatus status,
            Pageable pageable
    );

    Page<PromotionEntity> findAllByNameContainingIgnoreCaseAndStatusAndIsDeletedFalse(
            String keyword,
            PromotionStatus status,
            Pageable pageable
    );

    @Query("""
        SELECT p
        FROM PromotionEntity p
        WHERE p.isDeleted = false
          AND (:status IS NULL OR p.status = :status)
          AND LOWER(p.name) LIKE CONCAT('%', LOWER(:keyword), '%')
        """)
    Page<PromotionEntity> search(
            @Param("keyword") String keyword,
            @Param("status") PromotionStatus status,
            Pageable pageable);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PromotionEntity p WHERE p.id = :id AND p.isDeleted = false")
    Optional<PromotionEntity> findByIdForUpdate(@Param("id") UUID id);

}