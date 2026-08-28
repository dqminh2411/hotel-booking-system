package com.hotelbooking.chassis.audit;

/**
 * Loại đối tượng bị tác động bởi hành động audit.
 */
public enum TargetType {
    BOOKING,
    HOTEL,
    ROOM_TYPE,
    PAYMENT,
    USER,
    PROMOTION,
    NOTIFICATION,
    SYSTEM,
    OTHER
}
