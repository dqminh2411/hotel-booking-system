package com.hotelbooking.bookingservice.dto.kafka;

import com.hotelbooking.bookingservice.enums.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateBookingCommand(
    UUID sagaId,
    String eventType,
    User user,
    Hotel hotel,
    UUID bookingId,
    List<RoomTypeItem> roomTypeList,
    LocalDate checkin,
    LocalDate checkout,
    Integer numAdults,
    BigDecimal totalAmount,
    String currency,
    PaymentMethod paymentMethod,
    String paymentToken
) {
    public record User(UUID userId, String name, String email) {
    }

    public record Hotel(UUID hotelId, String name, String address) {
    }

    public record RoomTypeItem(
        UUID roomTypeId,
        String name,
        Integer bedCount,
        Integer bookingQuantity,
        Integer totalQuantity,
        BigDecimal price
    ) {
    }
}
