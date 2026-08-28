package com.hotelbooking.userservice.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.hotelbooking.userservice.dto.CreateSubPlanRequest;
import com.hotelbooking.userservice.dto.SubscriptionPlanResponse;
import com.hotelbooking.userservice.dto.UpdateSubPlanRequest;
import com.hotelbooking.userservice.entity.BillingCycle;

public interface SubscriptionService {

    /*=====Subscription Plan=====*/
    Page<SubscriptionPlanResponse> getListSubscription(String search, BillingCycle billingCycle, Pageable pageable);

    SubscriptionPlanResponse createSubscriptionPlan(CreateSubPlanRequest request);

    SubscriptionPlanResponse getPlanDetail(UUID planId);

    void deletePlan(UUID planId);

    SubscriptionPlanResponse updateSubscriptionPlan(UUID planId, UpdateSubPlanRequest request);
    /*================*/
}
