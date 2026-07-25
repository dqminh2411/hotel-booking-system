package com.hotelbooking.hotelservice.dto.response;
import java.util.UUID;

public record RoomTypeQuantityResponse(UUID roomTypeId, long totalQuantity) { }