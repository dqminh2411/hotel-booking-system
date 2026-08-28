package com.hotelbooking.userservice.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CreateTenantSubscriptionRequest(
    @NotNull(message = "Không được để trống tenantId")
    UUID tenantId,

    @NotNull(message = "Không được để trống subscriptionId")
    UUID subscriptionId
) {

}
