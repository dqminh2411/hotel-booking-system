package com.hotelbooking.hotelservice.controller;

import com.hotelbooking.hotelservice.dto.response.ApiResponse;
import com.hotelbooking.hotelservice.dto.response.HotelDetailsResponse;
import com.hotelbooking.hotelservice.dto.response.RoomTypeResponse;
import com.hotelbooking.hotelservice.service.HotelService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hotels")
@Validated
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HotelController {

    static final String ID_PATTERN = "^[A-Za-z0-9-]+$";

    private final HotelService hotelService;

    @GetMapping("/{hotelId}")
    public ApiResponse<HotelDetailsResponse> getHotelById(
            @PathVariable
            @Pattern(regexp = ID_PATTERN, message = "hotelId has invalid format")
            UUID hotelId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkinDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkoutDate,
            @RequestParam(required = false)
            @Min(value = 1, message = "guestNum phải lớn hơn 0")
            Integer guestNum,

            @RequestParam(required = false)
            @Min(value = 1, message = "roomNum phải lớn hơn 0")
            Integer roomNum
    ) {
        return ApiResponse.<HotelDetailsResponse>builder()
                            .code(200)
                            .message("Lấy thành công thông tin khách sạn: " + hotelId.toString())
                            .data(hotelService.getHotelDetail(hotelId, checkinDate, checkoutDate, guestNum, roomNum))
                            .build();
    }

}

