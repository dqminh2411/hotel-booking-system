package com.hotelbooking.hotelservice.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hotelbooking.hotelservice.dto.response.ApiResponse;
import com.hotelbooking.hotelservice.dto.response.RoomTypeDetailResponse;

import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/room-types")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomTypeController {

    static final String ID_PATTERN = "^[A-Za-z0-9-]+$";
    
    @GetMapping("/{id}")
    public ApiResponse<RoomTypeDetailResponse> getRoomTypeDetail(
            @PathVariable("id")
            @Pattern(regexp = ID_PATTERN, message = "hotelId has invalid format")
            UUID id){
        
                
        return ApiResponse.<RoomTypeDetailResponse>builder()
                            .code(200)
                            .message("Thông tin chi tiết loại phòng: " + id.toString())
                            .build();
    }
}
