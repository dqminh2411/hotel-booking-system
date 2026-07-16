package com.hotelbooking.userservice.service;

import com.hotelbooking.userservice.dto.*;
import com.hotelbooking.userservice.entity.UserEntity;

import java.util.UUID;

public interface UserService {
    
    UserResponse createUser(RegisterUserRequest request);
    UserResponse getCurrentUser(String authorizationHeader);

    UserResponse getUserById(UUID userId);

    UserResponse toResponse(UserEntity user);
}
