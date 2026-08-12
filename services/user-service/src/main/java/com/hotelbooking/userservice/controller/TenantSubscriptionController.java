package com.hotelbooking.userservice.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hotelbooking.userservice.dto.ApiResponse;
import com.hotelbooking.userservice.dto.CreateTenantSubscriptionRequest;
import com.hotelbooking.userservice.dto.UpdateTenantSubscription;
import com.hotelbooking.userservice.service.TenantSubscriptionService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/admin/tenants")
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TenantSubscriptionController {

    TenantSubscriptionService tenantSubscriptionService;

    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'HOTEL_OWNER')")
    @GetMapping("/{tenantId}/subscriptions")
    public ApiResponse<?> getSubscriptionTenant(
        @PathVariable(name = "tenantId") UUID tenantId,
        @RequestParam(name = "page", required = false, defaultValue = "0") @Min(0) int page,
        @RequestParam(name = "size", required = false, defaultValue = "10") @Min(10) @Max(30) int size,
        @AuthenticationPrincipal Jwt jwt
    ){
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "startedAt");
        UUID curentId = UUID.fromString(jwt.getSubject());
        boolean isAdmin = hasRealmRole(jwt, "PLATFORM_ADMIN");

        return ApiResponse.builder()
                        .code(200)
                        .message("Lấy thành công lịch sử sử dụng gói của tenantId = " + tenantId.toString())
                        .data(tenantSubscriptionService.getSubscriptionTenant(tenantId, curentId, isAdmin, pageable))
                        .build();
    }

    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'HOTEL_OWNER')")
    @PostMapping("/subscriptions")
    public ApiResponse<?> createTenantSubscription(
        @Valid @RequestBody CreateTenantSubscriptionRequest request,
        @AuthenticationPrincipal Jwt jwt
    ){
        UUID curentId = UUID.fromString(jwt.getSubject());
        boolean isAdmin = hasRealmRole(jwt, "PLATFORM_ADMIN");

        return ApiResponse.builder()
                        .code(201)
                        .message("Đăng ký subscription plan thành công cho tenantId = " + request.tenantId().toString())
                        .data(tenantSubscriptionService.createTenantSubscription(request, curentId, isAdmin))
                        .build();
    }

    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'HOTEL_OWNER')")
    @GetMapping("/subscriptions/{id}")
    public ApiResponse<?> getDetail(
        @PathVariable(name = "id") UUID id,
        @AuthenticationPrincipal Jwt jwt
    ){
        UUID curentId = UUID.fromString(jwt.getSubject());
        boolean isAdmin = hasRealmRole(jwt, "PLATFORM_ADMIN");

        return ApiResponse.builder()
                        .code(200)
                        .message("Lấy thành công chi tiết")
                        .data(tenantSubscriptionService.getDetail(id, curentId, isAdmin))
                        .build();
    }

    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'HOTEL_OWNER')")
    @PatchMapping("/subscriptions/{id}")
    public ApiResponse<?> updateStatus(
        @PathVariable(name = "id") UUID id,
        @Valid @RequestBody UpdateTenantSubscription request,
        @AuthenticationPrincipal Jwt jwt
    ){

        UUID currentId = UUID.fromString(jwt.getSubject());
        boolean isAdmin = hasRealmRole(jwt, "PLATFORM_ADMIN");

        return ApiResponse.builder()
                        .code(200)
                        .message("Cập nhật trạng thái thành công")
                        .data(tenantSubscriptionService.updateStatus(id, request, currentId, isAdmin))
                        .build();
    }

    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @DeleteMapping("/subscriptions/{id}")
    public ApiResponse<?> delete(@PathVariable(name = "id") UUID id){

        tenantSubscriptionService.delete(id);

        return ApiResponse.builder()
                        .code(200)
                        .message("Admin xóa thành công")
                        .build();
    }

    private boolean hasRealmRole(Jwt jwt, String role) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) return false;
        Object roles = realmAccess.get("roles");
        return roles instanceof List<?> list && list.contains(role);
    }

}