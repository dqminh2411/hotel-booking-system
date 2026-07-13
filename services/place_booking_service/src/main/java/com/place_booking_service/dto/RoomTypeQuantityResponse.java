package com.place_booking_service.dto;

import java.util.UUID;

public record RoomTypeQuantityResponse(UUID roomTypeId, long totalQuantity) { }
