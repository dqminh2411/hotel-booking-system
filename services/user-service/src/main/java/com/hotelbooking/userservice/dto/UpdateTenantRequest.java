package com.hotelbooking.userservice.dto;

import com.hotelbooking.userservice.entity.TenantStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTenantRequest(
    @NotBlank(message = "Tên phải có ký tự")
    @Size(min = 20, message = "Độ dài tên không phù hợp")
    String name,
    
    TenantStatus status
) {

}
