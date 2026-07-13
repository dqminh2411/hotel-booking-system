package com.hotelbooking.userservice.controller;

import com.hotelbooking.userservice.dto.AccessTokenResponse;
import com.hotelbooking.userservice.dto.AuthResponse;
import com.hotelbooking.userservice.dto.GoogleLoginRequest;
import com.hotelbooking.userservice.dto.LoginRequest;
import com.hotelbooking.userservice.dto.LogoutRequest;
import com.hotelbooking.userservice.dto.LogoutResponse;
import com.hotelbooking.userservice.dto.RefreshTokenRequest;
import com.hotelbooking.userservice.dto.UserResponse;
import com.hotelbooking.userservice.dto.VerifyEmailRequest;
import com.hotelbooking.userservice.service.AuthService;
import com.hotelbooking.userservice.service.JwtService;
import com.hotelbooking.userservice.service.NotificationDeviceTokenClient;
import com.hotelbooking.userservice.service.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping({"/api/auth"})
public class AuthController {
    private final UserService userService;
    private final AuthService authService;
    

    public AuthController(
            UserService userService,
            AuthService authService
    ) {
        this.userService = userService;
        this.authService = authService;
    }

    @PostMapping({"/verify-email"})
    public UserResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return userService.verifyEmail(request.token());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }

    @PostMapping("/refresh-token")
    public AccessTokenResponse refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return userService.refreshAccessToken(request.refreshToken());
    }

    @PostMapping("/google")
    public AuthResponse loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        return authService.loginWithGoogle(request);
    }

    @PostMapping("/logout")
    public LogoutResponse logout(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody LogoutRequest request
    ) {
        return authService.logout(authorization, request.fcmToken());
    }
}
