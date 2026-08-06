package com.hotelbooking.userservice.dto;

import java.time.Instant;
import java.util.UUID;

import com.hotelbooking.userservice.entity.TenantStatus;
import com.hotelbooking.userservice.entity.TenantSubscriptionPlanStatus;

public record TenantDetailResponse(
    UUID tenantId,
    String name,
    TenantStatus status,
    UUID ownerId,
    String ownerEmail,
    String ownerFullname,
    String ownerPhone,
    Instant createdAt,
    Instant updatedAt,
    ActiveSubscription activeSubscription
) {
    public record ActiveSubscription(
        UUID id,
        UUID subscriptionPlanId,
        String planCode,
        String planName,
        TenantSubscriptionPlanStatus status,
        Instant startedAt,
        Instant expiresAt,
        Instant createdAt
    ) {
    }
}
