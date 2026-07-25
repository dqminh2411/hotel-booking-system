package com.place_booking_service.dto;

import java.util.UUID;

public record DuplicateRequestResult(
    boolean isDuplicated,
    UUID bookingId
) {

}
