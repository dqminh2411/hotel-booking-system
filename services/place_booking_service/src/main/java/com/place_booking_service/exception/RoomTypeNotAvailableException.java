package com.place_booking_service.exception;

import org.springframework.http.HttpStatus;
import java.util.UUID;

public class RoomTypeNotAvailableException extends PlaceBookingException {

    public RoomTypeNotAvailableException(UUID roomTypeId) {
        super(
            "ROOM_TYPE_NOT_AVAILABLE",
            "Room type " + roomTypeId + " does not have enough available rooms",
            HttpStatus.BAD_REQUEST
        );
    }
}
