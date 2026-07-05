package com.hotelbooking.userservice.service;

import com.hotelbooking.userservice.dto.*;
import com.hotelbooking.userservice.entity.UserEntity;

import java.util.UUID;

public interface UserService {
    UserResponse register(RegisterUserRequest request);

    UserResponse verifyEmail(String token);

    AuthResponse login(LoginRequest request);

    AccessTokenResponse refreshAccessToken(String refreshToken);

    UserResponse getCurrentUser(String authorizationHeader);

    UserResponse getUserById(UUID userId);

    UserResponse toResponse(UserEntity user);
}
