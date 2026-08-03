package com.hotelbooking.hotelservice.dto.request;

import com.hotelbooking.hotelservice.constant.HotelStatus;

import jakarta.validation.constraints.NotNull;

public record HotelUpdateStatusRequest(
    @NotNull(message = "Status mới không được thiếu")
    HotelStatus hotelStatus,
    String reason
) {

}
