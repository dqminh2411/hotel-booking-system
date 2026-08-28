package com.hotelbooking.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LockRequest(
    @NotNull(message = "Lý do không được bỏ trống khi thao tác")
    @NotBlank(message = "Lý do hợp lệ, được là khoảng trắng")
    String reason
) {

}
