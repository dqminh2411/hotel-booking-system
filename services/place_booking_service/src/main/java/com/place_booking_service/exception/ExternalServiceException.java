package com.place_booking_service.exception;

import org.springframework.http.HttpStatus;

public class ExternalServiceException extends PlaceBookingException {

    public ExternalServiceException(String serviceName, String message) {
        super(
            "EXTERNAL_SERVICE_ERROR",
            serviceName + " error: " + message,
            HttpStatus.BAD_GATEWAY
        );
    }
}
