package com.hotelbooking.bookingservice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.hotelbooking.bookingservice.enums.BookingStatus;

public record BookingCheckinInfo(
    UUID bookingId,
    BookingStatus status,
    String customerName,
    String customerEmail,
    LocalDate checkinDate,
    LocalDate checkoutDate,
    Integer numAdults,
    BigDecimal totalAmount
) {

}
