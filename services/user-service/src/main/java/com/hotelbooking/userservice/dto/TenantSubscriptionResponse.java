package com.hotelbooking.userservice.dto;

import java.time.Instant;
import java.util.UUID;

import com.hotelbooking.userservice.entity.BillingCycle;
import com.hotelbooking.userservice.entity.TenantSubscriptionPlanStatus;

public record TenantSubscriptionResponse(
    UUID id,
    UUID tenantId,
    UUID subscriptionId,
    String planCode,
    String planName,
    String planDescription,
    BillingCycle billingCycle,
    TenantSubscriptionPlanStatus status,
    Instant startedAt,
    Instant expiresAt,
    Instant createdAt
) {

}
