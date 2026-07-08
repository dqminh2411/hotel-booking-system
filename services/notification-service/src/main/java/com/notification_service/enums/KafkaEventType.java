package com.notification_service.enums;

public enum KafkaEventType {
    SEND_BOOKING_CONFIRMED("SendBookingConfirmed"),
    SEND_BOOKING_CANCELLED("SendBookingCancelled"),
    SEND_BOOKING_FAILED("SendBookingFailed");

    private final String eventType;
    KafkaEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventType() {
        return eventType;
    }
}
