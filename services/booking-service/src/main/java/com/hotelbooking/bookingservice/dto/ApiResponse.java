package com.hotelbooking.bookingservice.dto;

import lombok.Builder;

@Builder
public record ApiResponse<T>(
    int code,
    String message,
    T data
) {

}
