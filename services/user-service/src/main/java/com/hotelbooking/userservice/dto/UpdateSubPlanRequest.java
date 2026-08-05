package com.hotelbooking.userservice.dto;
import java.math.BigDecimal;

import com.hotelbooking.userservice.entity.BillingCycle;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

public record UpdateSubPlanRequest(
    
    @Size(max = 255, message = "Tên gói không được vượt quá 255 ký tự")
    String name, 

    @Size(max = 500, message = "Mô tả không được vượt quá 500 ký tự")
    String description,

    @DecimalMin(value = "0.0", inclusive = true, message = "Giá không được nhỏ hơn 0")
    @Digits(integer = 12, fraction = 2, message = "Giá trị không hợp lệ")
    BigDecimal price,

    BillingCycle billingCycle
) {}
