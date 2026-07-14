package com.place_booking_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSucceeded {
    private String eventType; // "PaymentSucceeded"
    private UUID sagaId;
    private UUID bookingId;
    private String paymentId;
    private BigDecimal amount;
    private String currency;
    private String transactionRef;
    private String processedAt;
}
