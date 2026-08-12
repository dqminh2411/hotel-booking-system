package com.hotelbooking.userservice.service.impl;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hotelbooking.userservice.dto.CreateTenantSubscriptionRequest;
import com.hotelbooking.userservice.dto.TenantSubscriptionDetailResponse;
import com.hotelbooking.userservice.dto.TenantSubscriptionResponse;
import com.hotelbooking.userservice.dto.UpdateTenantSubscription;
import com.hotelbooking.userservice.entity.BillingCycle;
import com.hotelbooking.userservice.entity.SubscriptionPlan;
import com.hotelbooking.userservice.entity.Tenant;
import com.hotelbooking.userservice.entity.TenantSubscription;
import com.hotelbooking.userservice.entity.TenantSubscriptionPlanStatus;
import com.hotelbooking.userservice.exception.ApiException;
import com.hotelbooking.userservice.repository.SubscriptionPlanRepository;
import com.hotelbooking.userservice.repository.TenantRepository;
import com.hotelbooking.userservice.repository.TenantSubscriptionRepository;
import com.hotelbooking.userservice.service.TenantSubscriptionService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TenantSubscriptionServiceImpl implements TenantSubscriptionService{

    TenantRepository tenantRepository;
    SubscriptionPlanRepository subscriptionPlanRepository;
    TenantSubscriptionRepository tenantSubscriptionRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<TenantSubscriptionResponse> getSubscriptionTenant(UUID tenantId, UUID currentId, boolean isAdmin, Pageable pageable){
        Tenant tenant = tenantRepository.findByIdAndIsDeletedFalse(tenantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND", "Không tồn tại tenantId = " + tenantId.toString()));

        if(!isAdmin && !currentId.equals(tenant.getOwnerUser().getId())){
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Bạn không có quyền thao tác trên subscription của tenant khác");
        }

        Page<TenantSubscription> tenantSubs = tenantSubscriptionRepository.findByTenant_IdAndIsDeletedFalse(tenantId, pageable);

        return tenantSubs.map(
            ts -> totenantSubscriptionResponse(ts)
        );
    }

    @Override
    @Transactional
    public TenantSubscriptionDetailResponse createTenantSubscription(CreateTenantSubscriptionRequest request, UUID currentId, boolean isAdmin){
        if(tenantSubscriptionRepository.existsByTenant_IdAndSubscriptionPlan_IdAndStatusAndIsDeletedFalse(request.tenantId(), request.subscriptionId(), TenantSubscriptionPlanStatus.ACTIVE)){
            throw new ApiException(HttpStatus.CONFLICT, "TENANT_ALREADY_USE_PLAN", "Tenant đang sử dụng plan này rồi");
        }

        Tenant tenant = tenantRepository.findByIdAndIsDeletedFalse(request.tenantId())
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND", "Không tồn tại tenantId = " + request.tenantId().toString()));

        if(!isAdmin && !currentId.equals(tenant.getOwnerUser().getId())){
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Bạn không có quyền thao tác trên subscription của tenant khác");
        }

        SubscriptionPlan subscriptionPlan = subscriptionPlanRepository.findByIdAndIsDeletedFalse(request.subscriptionId())
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SUBSCRIPTION_PLAN_NOT_FOUND", "Không tồn tại subscription = " + request.subscriptionId().toString()));

        TenantSubscription tenantSubscription = tenantSubscriptionRepository.findByTenant_IdAndStatusAndIsDeletedFalse(request.tenantId(), TenantSubscriptionPlanStatus.ACTIVE)
                                .orElse(null);

        if(tenantSubscription != null){
            tenantSubscription.setStatus(TenantSubscriptionPlanStatus.CANCELLED);
            tenantSubscription.setExpiresAt(Instant.now());

            tenantSubscriptionRepository.save(tenantSubscription);
        }

        Instant time = subscriptionPlan.getBillingCycle() == BillingCycle.MONTHLY
                                ? Instant.now().atZone(ZoneId.of("UTC")).plusMonths(1).toInstant()
                                : Instant.now().atZone(ZoneId.of("UTC")).plusYears(1).toInstant();

        TenantSubscription newTenantSubscription = new TenantSubscription(
            UUID.randomUUID(),
            tenant,
            subscriptionPlan,
            TenantSubscriptionPlanStatus.ACTIVE,
            Instant.now(),
            time,
            Instant.now(),
            false
        );

        tenantSubscriptionRepository.save(newTenantSubscription);

        return toTenantSubscriptionDetailResponse(newTenantSubscription);
    }

    @Override
    @Transactional(readOnly = true)
    public TenantSubscriptionDetailResponse getDetail(UUID tenantSubscriptionId, UUID currentId, boolean isAdmin){
        TenantSubscription tenantSubscription = tenantSubscriptionRepository.findByIdAndIsDeletedFalse(tenantSubscriptionId)
                            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TENANT_SUBSCRIPTION_NOT_FOUND", "Không tìm thấy bản ghi yêu cầu id = " + tenantSubscriptionId.toString()));

        if(!isAdmin){
            UUID ownerId = tenantSubscription.getTenant().getOwnerUser().getId();
            if(!currentId.equals(ownerId)){
                throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Bạn không có quyền thao tác trên subscription của tenant khác");
            }
        }

        return toTenantSubscriptionDetailResponse(tenantSubscription);
    }

    @Override
    @Transactional
    public TenantSubscriptionDetailResponse updateStatus(UUID id, UpdateTenantSubscription request, UUID currentId, boolean isAdmin){
        TenantSubscription tenantSubscription = tenantSubscriptionRepository.findByIdAndIsDeletedFalse(id)
                            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TENANT_SUBSCRIPTION_NOT_FOUND", "Không tìm thấy bản ghi yêu cầu id = " + id.toString()));

        if(!isAdmin){
            UUID ownerId = tenantSubscription.getTenant().getOwnerUser().getId();
            if(!currentId.equals(ownerId)){
                throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Bạn không có quyền thao tác trên subscription của tenant khác");
            }
        }

        if (tenantSubscription.getStatus() != TenantSubscriptionPlanStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, "SUBSCRIPTION_NOT_ACTIVE", "Chỉ có thể đổi trạng thái từ ACTIVE");
        }

        List<TenantSubscriptionPlanStatus> allow = isAdmin
                    ? List.of(TenantSubscriptionPlanStatus.CANCELLED, TenantSubscriptionPlanStatus.SUSPENDED)
                    : List.of(TenantSubscriptionPlanStatus.CANCELLED);

        if(!allow.contains(request.status())){
            throw new ApiException(HttpStatus.FORBIDDEN, "STATUS_NOT_ALLOWED", "Role hiện tại không được phép chuyển sang trạng thái " + request.status());
        }

        tenantSubscription.setStatus(request.status());
        tenantSubscription.setExpiresAt(Instant.now());

        return toTenantSubscriptionDetailResponse(tenantSubscription);
    }

    @Override
    @Transactional
    public void delete(UUID id){
        TenantSubscription tenantSubscription = tenantSubscriptionRepository.findByIdAndIsDeletedFalse(id)
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TENANT_SUBSCRIPTION_NOT_FOUND", "Không tìm thấy bản ghi yêu cầu id = " + id.toString()));

        if(tenantSubscription.getStatus() == TenantSubscriptionPlanStatus.ACTIVE) tenantSubscription.setStatus(TenantSubscriptionPlanStatus.SUSPENDED);
        tenantSubscription.setIsDeleted(true);
    }

    private TenantSubscriptionResponse totenantSubscriptionResponse(TenantSubscription tenantSubscription){
        return new TenantSubscriptionResponse(
            tenantSubscription.getId(),
            tenantSubscription.getTenant().getId(),
            tenantSubscription.getSubscriptionPlan().getId(),
            tenantSubscription.getSubscriptionPlan().getCode(),
            tenantSubscription.getSubscriptionPlan().getName(),
            tenantSubscription.getSubscriptionPlan().getDescription(),
            tenantSubscription.getSubscriptionPlan().getBillingCycle(),
            tenantSubscription.getStatus(),
            tenantSubscription.getStartedAt(),
            tenantSubscription.getExpiresAt(),
            tenantSubscription.getCreatedAt()
        );
    }

    private TenantSubscriptionDetailResponse toTenantSubscriptionDetailResponse(TenantSubscription tenantSubscription){
        return new TenantSubscriptionDetailResponse(
            tenantSubscription.getId(),
            tenantSubscription.getTenant().getId(),
            tenantSubscription.getSubscriptionPlan().getId(),
            tenantSubscription.getTenant().getName(),
            tenantSubscription.getTenant().getStatus(),
            tenantSubscription.getSubscriptionPlan().getCode(),
            tenantSubscription.getSubscriptionPlan().getName(),
            tenantSubscription.getSubscriptionPlan().getDescription(),
            tenantSubscription.getSubscriptionPlan().getBillingCycle(),
            tenantSubscription.getStatus(),
            tenantSubscription.getStartedAt(),
            tenantSubscription.getExpiresAt(),
            tenantSubscription.getCreatedAt()
        );
    }
}