package com.hotelbooking.bookingservice.exception;

public class InsufficientRoomException extends RuntimeException {
    public InsufficientRoomException(String message) {
        super(message);
    }
}
