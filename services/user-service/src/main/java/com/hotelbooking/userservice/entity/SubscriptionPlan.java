package com.hotelbooking.userservice.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "subscription_plans")
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class SubscriptionPlan {

    @Id
    @Column(name = "id")
    @EqualsAndHashCode.Include
    @ToString.Include
    UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    String code;

    @Column(name = "name", nullable = false, length = 255)
    String name;

    @Column(name = "description", nullable = true, length = 500)
    String description;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    BigDecimal price = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "billing_cycle", nullable = false)
    BillingCycle billingCycle;

    @Column(name = "created_at", nullable = false)
    Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt = Instant.now();

    @Column(name = "is_deleted", nullable = false)
    Boolean isDeleted = false;
}
