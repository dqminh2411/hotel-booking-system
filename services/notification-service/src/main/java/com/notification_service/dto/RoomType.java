package com.notification_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RoomType(
    UUID roomTypeId,
    String name,
    int bedCount,
    int bookingQuantity,
    int totalQuantity,
    BigDecimal price
) {
}
