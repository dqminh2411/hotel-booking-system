package com.hotelbooking.bookingservice.dto.kafka;

import com.hotelbooking.bookingservice.enums.PaymentMethod;
import java.math.BigDecimal;
import java.util.UUID;

public record BookingCreated(
    UUID sagaId,
    String eventType,
    UUID bookingId,
    UUID userId,
    BigDecimal totalAmount,
    String currency,
    PaymentMethod paymentMethod,
    String paymentToken
) {
}

