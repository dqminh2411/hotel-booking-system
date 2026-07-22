package com.promotion.promotion_service.entity;


import com.promotion.promotion_service.constant.promotions.PromotionDiscountType;
import com.promotion.promotion_service.constant.promotions.PromotionStatus;
import com.promotion.promotion_service.constant.promotions.PromotionType;
import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "promotions")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql =
        "UPDATE promotions SET is_deleted=true, deleted_at=NOW() WHERE id=?") // soft deleted
public class PromotionEntity {

    @Id
    @Column(name = "id", nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "name", nullable = false)
    @ToString.Include
    private String name;

    @Column(name = "description")
    @ToString.Include
    private String description;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private PromotionType type;

    @Column(name = "discount_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private PromotionDiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "max_discount_amount")
    private BigDecimal maxDiscountAmount;

    @Column(name = "min_booking_amount")
    private BigDecimal minBookingAmount;

    @Column(name = "min_nights")
    private Integer minNights;

    @Column(name = "start_at", nullable = false)
    private OffsetDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private OffsetDateTime endAt;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private PromotionStatus status;

    @Column(name = "total_usage_limit")
    private Integer totalUsageLimit;

    @Column(name = "per_user_usage_limit")
    private Integer perUserUsageLimit;

    @Column(name = "current_usage_count")
    private Integer currentUsageCount;

    @Column(name = "stackable", nullable = false)
    private boolean stackable;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private OffsetDateTime updated_at;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}
