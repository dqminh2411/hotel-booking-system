package com.place_booking_service.exception;

import org.springframework.http.HttpStatus;

public class SagaStateConflictException extends PlaceBookingException {

    public SagaStateConflictException(String idempotencyKey) {
        super(
            "SAGA_STATE_CONFLICT",
            "Saga state is inconsistent for idempotencyKey: " + idempotencyKey,
            HttpStatus.CONFLICT
        );
    }
}
