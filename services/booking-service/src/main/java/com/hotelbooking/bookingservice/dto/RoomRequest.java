package com.hotelbooking.bookingservice.dto;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RoomRequest(
        @JsonProperty("room_type_id") UUID roomTypeId,
        @JsonProperty("quantity") Integer quantity
) {
}
