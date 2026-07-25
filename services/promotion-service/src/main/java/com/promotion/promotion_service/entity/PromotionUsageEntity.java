package com.promotion.promotion_service.entity;

import com.promotion.promotion_service.constant.promotion_usage.PromotionUsageStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "promotion_usages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PromotionUsageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    @ToString.Include
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false)
    PromotionEntity promotion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    CouponEntity coupon;

    @Column(name = "booking_id", nullable = false)
    UUID bookingId;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(name = "hotel_id", nullable = false)
    UUID hotelId;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    BigDecimal discountAmount;

    @Column(name = "booking_amount", nullable = false, precision = 12, scale = 2)
    BigDecimal bookingAmount;

    @Column(name = "used_at", nullable = false)
    OffsetDateTime usedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    PromotionUsageStatus status;

    @Column(name = "idempotency_key", length = 100)
    String idempotencyKey;

}