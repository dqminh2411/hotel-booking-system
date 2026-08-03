package com.hotelbooking.userservice.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.hotelbooking.userservice.dto.CreateUserAdminRequest;
import com.hotelbooking.userservice.dto.LockRequest;
import com.hotelbooking.userservice.dto.ResponseUser;
import com.hotelbooking.userservice.dto.UpdateUserRequest;
import com.hotelbooking.userservice.dto.UserResponse;
import com.hotelbooking.userservice.entity.UserStatus;

public interface AdminService {

    Page<ResponseUser> getAllUsers(UserStatus status, String search, Pageable pageable);

    UserResponse getUserDetail(UUID userId);

    UserResponse createUserAdmin(CreateUserAdminRequest request);

    UserResponse updateUser(UUID userId, UpdateUserRequest request);

    void deleteSafeUser(UUID userId, UUID adminId);

    ResponseUser lockUser(UUID userId, UUID adminId, LockRequest request);

    ResponseUser unlockUser(UUID userId);
}
