package com.hotelbooking.hotelservice.dto.request;

import java.util.UUID;

public record EmailHotelStatusRequest(
    User tenant,
    Hotel hotel,

    String eventType,
    String reason
) {
    public record User(
        UUID userId, 
        String name,
        String email
    ){};

    public record Hotel(
        UUID hotelId,
        String name,
        String address
    ) {
    }
}
