package com.hotelbooking.hotelservice.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.hotelbooking.hotelservice.constant.AdjustmentType;
import com.hotelbooking.hotelservice.constant.PricingRuleType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Entity
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "pricing_rules")
public class PricingRuleEntity {
    @Id
    @Column(name = "id", nullable = false, length = 255)
    @EqualsAndHashCode.Include
    @ToString.Include
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_type_id", nullable = false)
    RoomTypeEntity roomType;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    PricingRuleType type;

    @Column(name = "start_date", nullable = false)
    LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    LocalDate endDate;

    @Column(name = "adjustment_type", nullable = false)
    @Enumerated(EnumType.STRING)
    AdjustmentType adjustmentType;

    @Column(name = "price_value", nullable = false)
    BigDecimal priceValue;

    @Column(name = "priority", nullable = false)
    Integer priority;

    @Column(name = "is_deleted", nullable = false)
    Boolean isDeleted = false;
}
