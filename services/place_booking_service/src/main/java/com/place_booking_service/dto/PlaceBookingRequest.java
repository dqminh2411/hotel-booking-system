package com.place_booking_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceBookingRequest {

    @NotNull
    private UUID userId;

    @NotNull
    private UUID hotelId;

    @Valid
    @NotEmpty
    private List<RoomType> roomTypeList;

    @NotBlank
    private String checkin;

    @NotBlank
    private String checkout;

    @Min(1)
    private int numAdults;

    @Positive
    private BigDecimal totalAmount;

    @NotBlank
    private String currency;

    @NotBlank
    private String paymentMethod;

    @NotBlank
    private String idempotencyKey;

    @NotBlank
    private String paymentToken;

    private String forceToken;

    private String couponCode;
}
