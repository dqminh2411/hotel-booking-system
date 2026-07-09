package com.hotelbooking.userservice.controller;

import com.hotelbooking.userservice.dto.*;
import com.hotelbooking.userservice.service.AuthService;
import com.hotelbooking.userservice.service.UserService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping({"/api/auth"})
public class AuthController {
    private final UserService userService;
    private final AuthService authService;

    public AuthController(UserService userService,
                          AuthService authService) {
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
}
