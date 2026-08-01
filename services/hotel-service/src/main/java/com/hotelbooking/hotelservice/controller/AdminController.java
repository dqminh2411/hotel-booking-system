package com.hotelbooking.hotelservice.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hotelbooking.hotelservice.dto.request.HotelImageDelRequest;
import com.hotelbooking.hotelservice.dto.request.HotelUpdateStatusRequest;
import com.hotelbooking.hotelservice.dto.response.ApiResponse;
import com.hotelbooking.hotelservice.dto.response.HotelDetailsResponse;
import com.hotelbooking.hotelservice.dto.response.HotelPendingResponse;
import com.hotelbooking.hotelservice.service.AdminService;
import com.hotelbooking.hotelservice.service.HotelService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/admin/hotels")
@Validated
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminController {

    AdminService adminService;
    HotelService hotelService;

    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @GetMapping("/pending")
    public ApiResponse<Page<HotelPendingResponse>> getListHotelPending(
        @RequestParam(name = "page", defaultValue = "0", required = false) @Min(0) int page,
        @RequestParam(name = "size", defaultValue = "10", required = false) @Min(10) @Max(30) int size
    ){
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));

        Page<HotelPendingResponse> result = adminService.getListHotelPending(pageable);
        return ApiResponse.<Page<HotelPendingResponse>>builder()
                        .code(200)
                        .message("Lấy thành công danh sách khách sạn đăng ký vào hệ thống")
                        .data(result)
                        .build();
    }
    
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @PatchMapping("/{hotelId}/status")
    public ApiResponse<?> updateHotelStatus(
        @PathVariable(name = "hotelId") UUID hotelId,
        @Valid @RequestBody HotelUpdateStatusRequest request 
    ){

        adminService.updateHotelStatus(hotelId, request);

        return ApiResponse.builder()
                        .code(200)
                        .message("Cập nhật trạng thái hotel thành công")
                        .build();
    }

    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @DeleteMapping("/images")
    public ApiResponse<?> deleteHotelImages(
        @Valid @RequestBody HotelImageDelRequest request
    ){
        adminService.deleteHotelImages(request);
        return ApiResponse.builder().build();
    }

    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @GetMapping("/{hotelId}")
    public ApiResponse<HotelDetailsResponse> getHotelById(
            @PathVariable(name = "hotelId") UUID hotelId){
        return ApiResponse.<HotelDetailsResponse>builder()
                .code(200)
                .message("Lấy thành công thông tin khách sạn: " + hotelId.toString() + "cho Admin")
                .data(hotelService.getHotelDetailForAdmin(hotelId))
                .build();
    }
}
