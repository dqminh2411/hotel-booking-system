package com.promotion.promotion_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidatePromotionPreRequest {
    @NotBlank
    private String couponCode;

    @NotNull
    private UUID userId;

    @NotNull
    private UUID hotelId;

    private List<UUID> roomTypeIds;
    private String checkin;
    private String checkout;

    @NotNull
    @Positive
    private BigDecimal totalAmount;
}
