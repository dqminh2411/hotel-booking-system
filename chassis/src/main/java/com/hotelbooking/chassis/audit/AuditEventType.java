package com.hotelbooking.chassis.audit;

/**
 * Các loại sự kiện audit trong hệ thống.
 * Sử dụng UPPER_SNAKE_CASE.
 */
public enum AuditEventType {

    // Booking
    CREATE_BOOKING,
    CANCEL_BOOKING,
    UPDATE_BOOKING,
    CONFIRM_BOOKING,

    // Hotel
    CREATE_HOTEL,
    UPDATE_HOTEL,
    DELETE_HOTEL,
    APPROVE_HOTEL,
    REJECT_HOTEL,

    // Room
    CREATE_ROOM_TYPE,
    UPDATE_ROOM_TYPE,
    DELETE_ROOM_TYPE,

    // Payment
    PROCESS_PAYMENT,
    REFUND_PAYMENT,

    // User
    CREATE_USER,
    UPDATE_USER,
    DELETE_USER,
    LOGIN,
    LOGOUT,

    // Promotion
    CREATE_PROMOTION,
    UPDATE_PROMOTION,
    DELETE_PROMOTION,
    APPLY_PROMOTION,

    // Notification
    SEND_NOTIFICATION,

    // Generic
    CUSTOM
}
