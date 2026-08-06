package com.hotelbooking.userservice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.hotelbooking.userservice.entity.BillingCycle;

public record SubscriptionPlanResponse(
    UUID id,
    String code,
    String name,
    String description,
    BigDecimal price,
    BillingCycle billingCycle,
    Instant createdAt,
    Instant updatedAt
) {

}
