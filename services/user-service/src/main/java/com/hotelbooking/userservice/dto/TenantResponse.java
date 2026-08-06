package com.hotelbooking.userservice.dto;

import java.time.Instant;
import java.util.UUID;

import com.hotelbooking.userservice.entity.TenantStatus;

public record TenantResponse(
    UUID tenantId,
    String name,
    TenantStatus status,
    UUID ownerId,
    String ownerEmail,
    String ownerFullname,
    String ownerPhone,
    Instant createdAt
) {

}
