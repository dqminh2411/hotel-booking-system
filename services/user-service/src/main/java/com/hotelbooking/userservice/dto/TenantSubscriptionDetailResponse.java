package com.hotelbooking.userservice.dto;

import java.time.Instant;
import java.util.UUID;

import com.hotelbooking.userservice.entity.BillingCycle;
import com.hotelbooking.userservice.entity.TenantStatus;
import com.hotelbooking.userservice.entity.TenantSubscriptionPlanStatus;

public record TenantSubscriptionDetailResponse(
    UUID id,
    UUID tenantId,
    UUID subscriptionId,
    String tenantName,
    TenantStatus tenantStatus,
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
