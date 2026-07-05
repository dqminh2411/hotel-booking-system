package com.hotelbooking.userservice.controller;

import com.hotelbooking.userservice.dto.*;
import com.hotelbooking.userservice.service.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping({"/api/users", "/users"})
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterUserRequest request) {
        return userService.register(request);
    }

    @GetMapping("/me")
    public UserResponse getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return userService.getCurrentUser(authorization);
    }

    @GetMapping("/{userId}")
    public UserResponse getUserById(
            @PathVariable UUID userId
    ) {
        return userService.getUserById(userId);
    }
}
