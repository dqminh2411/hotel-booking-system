package com.place_booking_service.exception;

import org.springframework.http.HttpStatus;
import java.util.UUID;

public class RoomTypeNotFoundException extends PlaceBookingException {

    public RoomTypeNotFoundException(UUID roomTypeId) {
        super("ROOM_TYPE_NOT_FOUND", "Room type not found: " + roomTypeId, HttpStatus.NOT_FOUND);
    }
}
