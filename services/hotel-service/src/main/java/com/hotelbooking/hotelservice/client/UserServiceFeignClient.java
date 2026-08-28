package com.hotelbooking.hotelservice.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.hotelbooking.hotelservice.dto.response.UserResponse;

@FeignClient(name = "user-service", url = "http://user-service:5000", path = "/api/users")
public interface UserServiceFeignClient {

    @GetMapping("/{userId}")
    UserResponse getUserById(@PathVariable UUID userId);
}
