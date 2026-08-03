package com.hotelbooking.userservice.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
    @Size(max = 255)
    String fullName,

    @Pattern(
        regexp = "^0\\d{9}$",
        message = "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng số 0"
    )
    String phone,

    @Size(max = 500)
    String address,

    @Size(max = 500)
    String avatarUrl
) {

}
