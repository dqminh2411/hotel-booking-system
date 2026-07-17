package com.place_booking_service.exception;

import org.springframework.http.HttpStatus;
import java.util.UUID;

public class RoomTypeNotAvailableException extends PlaceBookingException {

    public RoomTypeNotAvailableException(UUID roomTypeId) {
        super(
            "ROOM_TYPE_NOT_AVAILABLE",
            "Có vẻ bạn đặt chậm tay mất rồi khi đặt phòng " + roomTypeId.toString() + ". Hãy chọn lại phòng khác nhé",
            HttpStatus.BAD_REQUEST
        );
    }
}
