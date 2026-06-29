package com.place_booking_service.exception;

import org.springframework.http.HttpStatus;

public class PlaceBookingException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public PlaceBookingException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
