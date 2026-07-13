package com.place_booking_service.exception;

import org.springframework.http.HttpStatus;
import java.util.UUID;

public class HotelNotFoundException extends PlaceBookingException {

    public HotelNotFoundException(String hotelId) {
        super("HOTEL_NOT_FOUND", "Hotel not found: " + hotelId, HttpStatus.NOT_FOUND);
    }
}
