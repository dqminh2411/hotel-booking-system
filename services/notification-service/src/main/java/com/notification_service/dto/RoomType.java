package com.notification_service.dto;

public record RoomType(
    String roomTypeId,
    String name,
    int bedCount,
    int bookingQuantity,
    int totalQuantity,
    double price
) {
}
