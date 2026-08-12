package com.hotelbooking.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserAdminRequest(
    @NotNull(message = "email không được để trống")
    @NotBlank(message = "Email phải có ký tự")
    String email,

    @NotNull(message = "password không được để trống")
    @NotBlank(message = "password phải có ký tự")
    @Size(min = 8)
    String password,

    @Pattern(
        regexp = "^0\\d{9}$",
        message = "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng số 0"
    )
    String phone,

    @NotNull(message = "Tên không được để trống")
    @NotBlank(message = "Tên phải có ít nhất 10 ký tự")
    @Size(min = 10, max = 255)
    String fullName,

    @NotNull(message = "Địa chỉ không được để trống")
    @NotBlank(message = "Địa chỉ phải có ít nhất 1 ký tự")
    @Size(max = 500)
    String address,

    @Size(max = 500)
    String avatarUrl,
    
    UserRole role
) {

}
