package com.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessPayment {
    private String eventType; // "ProcessPayment"
    private UUID sagaId;
    private UUID bookingId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String paymentToken;
    private String idempotencyKey;
    private UUID userId;
}
