package com.hotelbooking.hotelservice.dto.response;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        UUID userId,
        String email,
        String phone,
        String fullName,
        String name,
        String avatarUrl,
        String address,
        String status,
        List<String> roles,
        Instant createdAt,
        Instant updatedAt
) {
}