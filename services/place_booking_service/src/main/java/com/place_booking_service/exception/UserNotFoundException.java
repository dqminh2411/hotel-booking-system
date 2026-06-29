package com.place_booking_service.exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends PlaceBookingException {

    public UserNotFoundException(String userId) {
        super("USER_NOT_FOUND", "User not found: " + userId, HttpStatus.NOT_FOUND);
    }
}
