package com.hotelbooking.userservice.dto;

import java.time.Instant;
import java.util.UUID;

import com.hotelbooking.userservice.entity.UserStatus;

public record ResponseUser(
    UUID userId,
    String email,
    String phone,
    String fullname,
    String avatarUrl,
    UserStatus status,
    Instant createdAt
) {

}
