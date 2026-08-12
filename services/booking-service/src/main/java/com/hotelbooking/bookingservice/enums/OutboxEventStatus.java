package com.hotelbooking.bookingservice.enums;

public enum OutboxEventStatus {
    PENDING, PROCESSING, PUBLISHED, DEAD_LETTER
}
