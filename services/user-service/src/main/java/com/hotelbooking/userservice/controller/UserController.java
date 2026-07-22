package com.hotelbooking.userservice.controller;

import com.hotelbooking.userservice.dto.*;
import com.hotelbooking.userservice.service.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

@Validated
@RestController
@RequestMapping({"/api/users"})
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody CreateUserRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        UUID keycloakId = UUID.fromString(jwt.getSubject());
        return userService.createUser(keycloakId, request);
    }

    @GetMapping("/me")
    public UserResponse getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authentication token");
        }
        UUID userId = UUID.fromString(jwt.getSubject());
        return userService.getUserById(userId);
    }

    @GetMapping("/{userId}")
    public UserResponse getUserById(
            @PathVariable UUID userId
    ) {
        return userService.getUserById(userId);
    }
}
