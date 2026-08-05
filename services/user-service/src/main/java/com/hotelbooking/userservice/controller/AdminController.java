package com.hotelbooking.userservice.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
import com.hotelbooking.userservice.dto.CreateUserAdminRequest;
import com.hotelbooking.userservice.dto.LockRequest;
import com.hotelbooking.userservice.dto.ResponseUser;
import com.hotelbooking.userservice.dto.UpdateUserRequest;
import com.hotelbooking.userservice.dto.UserResponse;
import com.hotelbooking.userservice.entity.UserStatus;
import com.hotelbooking.userservice.service.AdminService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/admin/users")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class AdminController {

    AdminService adminService;

    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @GetMapping()
    public ApiResponse<Page<ResponseUser>> getAllUsers(
        @RequestParam(name = "page", defaultValue = "0", required = false) @Min(0) int page,
        @RequestParam(name =  "size", defaultValue = "10", required = false) @Min(10) @Max(30) int size,
        @RequestParam(name = "status", required = false) UserStatus status,
        @RequestParam(name = "search", required = false) String search
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.ASC, "createdAt");
        
        return ApiResponse.<Page<ResponseUser>>builder()
                        .code(200)
                        .message("Lấy thành công danh sách người dùng cho Admin")
                        .data(adminService.getAllUsers(status, search, pageable))
                        .build();
    }

    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @GetMapping("/{userId}")
    public ApiResponse<UserResponse> getUserDetail(@PathVariable(name = "userId") UUID userId){

        return ApiResponse.<UserResponse>builder()
                        .code(200)
                        .message("Lấy thành công chi tiết khách sạn cho Admin")
                        .data(adminService.getUserDetail(userId))
                        .build();
    }

    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @PostMapping("")
    public ApiResponse<?> createUserAdmin(@Valid @RequestBody CreateUserAdminRequest request){

        return ApiResponse.builder()
                        .code(201)
                        .message("Admin tạo người dùng mới thành công")
                        .data(adminService.createUserAdmin(request))
                        .build();
    }

    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @PatchMapping("/{userId}")
    public ApiResponse<?> updateUser(
        @PathVariable(name = "userId") UUID userId,
        @Valid @RequestBody UpdateUserRequest request
    ){

        return ApiResponse.builder()
                        .code(200)
                        .message("Cập nhật thông tin người dùng thành công")
                        .data(adminService.updateUser(userId, request))
                        .build();
    }

    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @DeleteMapping("/{userId}")
    public ApiResponse<?> deleteSafeUser(@PathVariable(name = "userId") UUID userId,
        @AuthenticationPrincipal Jwt jwt
    ){
        adminService.deleteSafeUser(userId, UUID.fromString(jwt.getSubject()));
        return ApiResponse.builder()
                        .code(200)
                        .message("Xóa mềm thành công tài khoản người dùng")
                        .build();
    }

    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @PatchMapping("/{userId}/lock")
    public ApiResponse<?> lockUser(
        @PathVariable(name = "userId") UUID userId,
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody LockRequest request
    ){

        return ApiResponse.builder()
                        .code(200)
                        .message("Khóa tài khoản người dùng thành công")
                        .data(adminService.lockUser(userId, UUID.fromString(jwt.getSubject()), request))
                        .build();
    }

    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @PatchMapping("/{userId}/unlock")
    public ApiResponse<?> unlockUser(
        @PathVariable(name = "userId") UUID userId
    ){

        return ApiResponse.builder()
                        .code(200)
                        .message("Mở khóa tài khoản người dùng thành công")
                        .data(adminService.unlockUser(userId))
                        .build();
    }
}
