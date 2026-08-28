package com.hotelbooking.userservice.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.hotelbooking.userservice.dto.CreateTenantSubscriptionRequest;
import com.hotelbooking.userservice.dto.TenantSubscriptionDetailResponse;
import com.hotelbooking.userservice.dto.TenantSubscriptionResponse;
import com.hotelbooking.userservice.dto.UpdateTenantSubscription;

public interface TenantSubscriptionService {

    Page<TenantSubscriptionResponse> getSubscriptionTenant(UUID tenantId, UUID currentId, boolean isAdmin, Pageable pageable);

    TenantSubscriptionDetailResponse createTenantSubscription(CreateTenantSubscriptionRequest request, UUID currentId, boolean isAdmin);

    TenantSubscriptionDetailResponse getDetail(UUID tenantSubscriptionId, UUID currentId, boolean isAdmin);

    TenantSubscriptionDetailResponse updateStatus(UUID id, UpdateTenantSubscription request, UUID currentId, boolean isAdmin);

    void delete(UUID id);
}
