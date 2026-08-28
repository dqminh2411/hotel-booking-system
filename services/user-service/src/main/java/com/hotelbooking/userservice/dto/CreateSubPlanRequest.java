package com.hotelbooking.userservice.dto;

import java.math.BigDecimal;

import com.hotelbooking.userservice.entity.BillingCycle;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSubPlanRequest(
    
    @NotBlank(message = "code không được để trống")
    @Size(max = 50, message = "code không được vượt quá 50 ký tự")
    String code,

    @NotBlank(message = "Tên không được để trống")
    @Size(max = 255, message = "Tên không được vượt quá 255 ký tự")
    String name, 

    @Size(max = 500, message = "Mô tả không được vượt quá 500 ký tự")
    String description,

    @NotNull(message = "Giá không được để trống")
    @DecimalMin(value = "0.0", inclusive = true, message = "Giá không được nhỏ hơn 0")
    @Digits(integer = 12, fraction = 2, message = "Giá trị không hợp lệ)")
    BigDecimal price,

    @NotNull(message = "Chu kỳ thanh toán không được để trống")
    BillingCycle billingCycle
) {}
