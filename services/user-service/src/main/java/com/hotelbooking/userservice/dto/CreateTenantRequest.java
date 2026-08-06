package com.hotelbooking.userservice.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTenantRequest(
    @NotNull(message = "Không được để trống id của người chủ")
    UUID ownerId,

    @NotNull(message = "Không được để trống tên doanh nghiệp")
    @NotBlank(message = "Tên doanh nghiệp phải có ký tự")
    String name
) {

}
