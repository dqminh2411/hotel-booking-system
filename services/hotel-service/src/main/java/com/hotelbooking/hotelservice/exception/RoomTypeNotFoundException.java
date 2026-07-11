package com.hotelbooking.hotelservice.exception;

public class RoomTypeNotFoundException extends RuntimeException {

    public RoomTypeNotFoundException(String roomTypeId) {
        super("Room type not found: " + roomTypeId + " in hotel");
    }
}

