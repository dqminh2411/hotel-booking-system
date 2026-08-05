package com.hotelbooking.userservice.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hotelbooking.userservice.dto.ApiResponse;
import com.hotelbooking.userservice.dto.CreateSubPlanRequest;
import com.hotelbooking.userservice.dto.UpdateSubPlanRequest;
import com.hotelbooking.userservice.entity.BillingCycle;
import com.hotelbooking.userservice.service.SubscriptionService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/subscription-plans")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class SubscriptionController {

    SubscriptionService subscriptionService;

    /*=====Subscription Plan=====*/
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @GetMapping("")
    public ApiResponse<?> getListSubscription(
        @RequestParam(name = "page", required = false, defaultValue = "0") @Min(0) int page,
        @RequestParam(name = "size", required = false, defaultValue = "10") @Min(10) @Max(30) int size,
        @RequestParam(name = "search", required = false, defaultValue = "") String search,
        @RequestParam(name = "billingCycle", required = false) BillingCycle billingCycle
    ){
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        return ApiResponse.builder()
                        .code(200)
                        .message("Lấy danh sách subscription plan thành công")
                        .data(subscriptionService.getListSubscription(search, billingCycle, pageable))
                        .build();
    }

    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @PostMapping("")
    public ApiResponse<?> createSubscriptionPlan(
        @Valid @RequestBody CreateSubPlanRequest request
    ){
        
        return ApiResponse.builder()
                        .code(201)
                        .message("Tạo subscription thành công")
                        .data(subscriptionService.createSubscriptionPlan(request))
                        .build();
    }

    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @GetMapping("/{planId}")
    public ApiResponse<?> getPlanDetail(@PathVariable(name = "planId") UUID planId){

        return ApiResponse.builder()
                        .code(200)
                        .message("Lấy thành công chi tiết planId = " + planId.toString())
                        .data(subscriptionService.getPlanDetail(planId))
                        .build();
    }

    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @DeleteMapping("/{planId}")
    public ApiResponse<?> deletePlan(@PathVariable(name = "planId") UUID planId){

        subscriptionService.deletePlan(planId);
        return ApiResponse.builder()
                        .code(200)
                        .message("Xóa thành công planId = " + planId.toString())
                        .build();
    }

    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @PatchMapping("/{planId}")
    public ApiResponse<?> updateSubscriptionPlan(
        @PathVariable(name = "planId") UUID planId,
        @Valid @RequestBody UpdateSubPlanRequest request
    ){

        return ApiResponse.builder()
                        .code(200)
                        .message("Cập nhật thành công gói plan")
                        .data(subscriptionService.updateSubscriptionPlan(planId, request))
                        .build();
    }

    /*================*/
}
