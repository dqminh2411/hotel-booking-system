package com.payment_service.dto;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRefunded {
    private String eventType; // "PaymentRefunded"
    private UUID sagaId;
    private UUID bookingId;
    private String paymentId;
    private BigDecimal amount;
    private String currency;
    private LocalDateTime refundedAt;
}
