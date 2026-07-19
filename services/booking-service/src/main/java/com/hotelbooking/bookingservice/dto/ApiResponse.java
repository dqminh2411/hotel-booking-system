package com.hotelbooking.bookingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor
public class ApiResponse<T>{
    int code;
    String message;
    T data;
}