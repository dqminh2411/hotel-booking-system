package com.hotelbooking.hotelservice.dto.response;

import java.time.Instant;
import java.util.UUID;

public record HotelPendingResponse(
    UUID hotelId,
    String name,
    UUID tenantId,
    Address address,
    String coverImgUrl,
    String status,
    Instant createdAt
) {

}
