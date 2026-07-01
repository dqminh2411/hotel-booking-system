package com.hotelbooking.userservice.dto;

public record AuthResponse(String accessToken, String refreshToken, UserResponse user) {
}
