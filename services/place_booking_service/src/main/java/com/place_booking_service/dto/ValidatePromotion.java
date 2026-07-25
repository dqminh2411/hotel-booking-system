package com.place_booking_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidatePromotion {
    @Builder.Default
    private String eventType = "ValidatePromotion";
    private UUID sagaId;
    private UUID bookingId;
    private UUID userId;
    private String couponCode;
    private UUID hotelId;
    private List<UUID> roomTypeIds;
    private String checkin;
    private String checkout;
    private BigDecimal totalAmount;
}
