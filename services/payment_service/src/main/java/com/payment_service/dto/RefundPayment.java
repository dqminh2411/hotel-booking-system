package com.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundPayment {
    private String eventType; // "RefundPayment"
    private UUID sagaId;
    private UUID bookingId;
    private String paymentId;
    private String reason;
}
