package com.place_booking_service.exception;

import org.springframework.http.HttpStatus;

public class InvalidBookingRequestException extends PlaceBookingException {

    public InvalidBookingRequestException(String message) {
        super("VALIDATION_ERROR", message, HttpStatus.BAD_REQUEST);
    }
}
