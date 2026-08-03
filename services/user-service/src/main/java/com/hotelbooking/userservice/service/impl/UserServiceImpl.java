package com.hotelbooking.userservice.service.impl;

import com.hotelbooking.chassis.logging.aop.Loggable;
import com.hotelbooking.chassis.logging.aop.LogParam;
import com.hotelbooking.userservice.dto.*;
import com.hotelbooking.userservice.entity.*;
import com.hotelbooking.userservice.exception.ApiException;
import com.hotelbooking.userservice.exception.UserNotFoundException;
import com.hotelbooking.userservice.repository.*;
import com.hotelbooking.userservice.service.*;

import lombok.AllArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    @Transactional
    @Loggable(event = "USER_REGISTERED", message = "Add new user")
    public UserResponse createUser(@LogParam("keycloakId") UUID keycloakId, CreateUserRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String phone = request.phone().trim();
        String fullName = request.fullName().trim();
        if (userRepository.existsByEmailIgnoreCaseOrPhoneOrId(email, phone, keycloakId)) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_USER_DATA",
                    "Email, phone or user already exists");
        }
        
        Instant now = Instant.now();
        UserEntity user = new UserEntity();
        user.setId(keycloakId);
        user.setEmail(email);
        user.setPhone(phone);
        user.setFullName(fullName);
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setDeleted(false);
        userRepository.save(user);
        return toResponse(user);
    }

    // @Override
    // @Transactional
    // public UserResponse verifyEmail(String token) {
    //     UUID userId = jwtService.parseEmailVerificationUserId(token);
    //     UserEntity user = userRepository.findWithRolesByIdAndDeletedFalse(userId)
    //             .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TOKEN",
    //                     "Verification token is invalid"));
    //     Instant now = Instant.now();
    //     if (user.isDeleted()) {
    //         throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TOKEN", "Verification token is invalid");
    //     }
    //     if (user.getStatus() == UserStatus.LOCKED) {
    //         throw new ApiException(HttpStatus.LOCKED, "ACCOUNT_LOCKED", "Account is locked");
    //     }
    //     user.setStatus(UserStatus.ACTIVE);
    //     user.setUpdatedAt(now);
    //     return toResponse(user);
    // }

    // @Override
    // @Transactional(readOnly = true)
    // public UserResponse getCurrentUser(String authorizationHeader) {
    //     return getUserById(jwtService.parseUserId(authorizationHeader));
    // }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        UserEntity user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return toResponse(user);
    }

    private void ensureCanAuthenticate(UserEntity user) {
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new ApiException(HttpStatus.LOCKED, "ACCOUNT_LOCKED", "Account is locked");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "EMAIL_NOT_VERIFIED",
                    "Email address has not been verified");
        }
    }


    public UserResponse toResponse(UserEntity user) {
        List<String> roles = new java.util.ArrayList<>();
        return new UserResponse(
                user.getId(), user.getId(), user.getEmail(), user.getPhone(),
                user.getFullName(), user.getFullName(), user.getAvatarUrl(), user.getAddress(),
                user.getStatus().name(), roles, user.getCreatedAt(), user.getUpdatedAt()
        );
    }

    @Override
    public UserResponse getCurrentUser(String authorizationHeader) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getCurrentUser'");
    }
}
