package com.hotelbooking.userservice.service.impl;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hotelbooking.userservice.dto.CreateSubPlanRequest;
import com.hotelbooking.userservice.dto.SubscriptionPlanResponse;
import com.hotelbooking.userservice.dto.UpdateSubPlanRequest;
import com.hotelbooking.userservice.entity.BillingCycle;
import com.hotelbooking.userservice.entity.SubscriptionPlan;
import com.hotelbooking.userservice.entity.TenantSubscriptionPlanStatus;
import com.hotelbooking.userservice.exception.ApiException;
import com.hotelbooking.userservice.repository.SubscriptionPlanRepository;
import com.hotelbooking.userservice.repository.TenantSubscriptionRepository;
import com.hotelbooking.userservice.service.SubscriptionService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubscriptionServiceImpl implements SubscriptionService{
    SubscriptionPlanRepository subscriptionPlanRepository;
    TenantSubscriptionRepository tenantSubscriptionRepository;

    /*=====Subscription Plan=====*/
    @Override
    @Transactional(readOnly = true)
    public Page<SubscriptionPlanResponse> getListSubscription(String search, BillingCycle billingCycle, Pageable pageable){
        
        Page<SubscriptionPlan> subscriptionPlans = subscriptionPlanRepository.findBySearch(search, billingCycle, pageable);
    
        return subscriptionPlans.map(
            sp -> toSubscriptionPlanResponse(sp)
        );
    }

    @Override
    @Transactional
    public SubscriptionPlanResponse createSubscriptionPlan(CreateSubPlanRequest request){
        if(subscriptionPlanRepository.existsByCodeOrNameAndIsDeletedFalse(request.code(), request.name())){
            throw new ApiException(HttpStatus.CONFLICT, "CODE_OR_NAME_ALREADY_EXISTS", "Code hoặc Name đã tồn tại");
        }

        SubscriptionPlan subscriptionPlan = new SubscriptionPlan(
            UUID.randomUUID(),
            request.code(),
            request.name(),
            request.description(),
            request.price(),
            request.billingCycle(),
            Instant.now(),
            Instant.now(),
            false
        );

        subscriptionPlanRepository.save(subscriptionPlan);

        return toSubscriptionPlanResponse(subscriptionPlan);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionPlanResponse getPlanDetail(UUID planId){
        SubscriptionPlan subscriptionPlan = subscriptionPlanRepository.findByIdAndIsDeletedFalse(planId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SUBSCRIPTION_PLAN_NOT_FOUND", "Không tìm thấy plan có id = " + planId.toString()));

        return toSubscriptionPlanResponse(subscriptionPlan);
    }

    @Override
    @Transactional
    public void deletePlan(UUID planId){

        if(tenantSubscriptionRepository.existsBySubscriptionPlan_IdAndStatusAndIsDeletedFalse(planId, TenantSubscriptionPlanStatus.ACTIVE)){
            throw new ApiException(HttpStatus.BAD_REQUEST, "SUB_PLAN_ALREADY_USED", "Không thể xóa plan đang có tenant sử dụng");
        }

        SubscriptionPlan subscriptionPlan = subscriptionPlanRepository.findByIdAndIsDeletedFalse(planId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SUBSCRIPTION_PLAN_NOT_FOUND", "Không tìm thấy plan có id = " + planId.toString()));

        subscriptionPlan.setIsDeleted(true);
        subscriptionPlanRepository.save(subscriptionPlan);
    }

    @Override
    @Transactional
    public SubscriptionPlanResponse updateSubscriptionPlan(UUID planId, UpdateSubPlanRequest request){
        if(request.name() == null && request.price() == null && request.description() == null && request.billingCycle() == null){
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Phải có ít nhất 1 field được cập nhật");
        }

        if(subscriptionPlanRepository.existByNameAndIdNotAndIsDeletedFalse(request.name(), planId)){
            throw new ApiException(HttpStatus.BAD_REQUEST, "CODE_ALREADY_EXISTS", "Code đã tồn tại trong hệ thống");
        }

        SubscriptionPlan subscriptionPlan = subscriptionPlanRepository.findByIdAndIsDeletedFalse(planId)
                            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SUB_PLAN_NOT_FOUND", "Không có sub plan tồn tại id = " + planId.toString()));

        if(request.name() != null) subscriptionPlan.setName(request.name());
        if(request.description() != null) subscriptionPlan.setDescription(request.description());
        if(request.price() != null) subscriptionPlan.setPrice(request.price());
        if(request.billingCycle() != null) subscriptionPlan.setBillingCycle(request.billingCycle());

        subscriptionPlanRepository.save(subscriptionPlan);

        return toSubscriptionPlanResponse(subscriptionPlan);
    }

    private SubscriptionPlanResponse toSubscriptionPlanResponse(SubscriptionPlan subscriptionPlan){

        return new SubscriptionPlanResponse(
            subscriptionPlan.getId(),
            subscriptionPlan.getCode(),
            subscriptionPlan.getName(),
            subscriptionPlan.getDescription(),
            subscriptionPlan.getPrice(),
            subscriptionPlan.getBillingCycle(),
            subscriptionPlan.getCreatedAt(),
            subscriptionPlan.getUpdatedAt()
        );
    }

    /*================*/
}
