package com.hotelbooking.userservice.dto;

import com.hotelbooking.userservice.entity.TenantSubscriptionPlanStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateTenantSubscription(
    @NotNull(message = "status không được để trống")
    TenantSubscriptionPlanStatus status
) {

}
