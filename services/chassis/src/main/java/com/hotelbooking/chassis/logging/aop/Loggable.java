package com.hotelbooking.chassis.logging.aop;

import java.lang.annotation.*;

/**
 * Đánh dấu method cần ghi structured business event log tự động.
 *
 * Aspect sẽ tự động log:
 *  - Khi method bắt đầu (với các param đã annotate @LogParam)
 *  - Khi method hoàn tất (với duration)
 *  - Khi method throw exception (với error detail)
 *
 * Mọi log đều kèm clientIp, traceId, userId, tenantId từ MDC.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Loggable {

    /**
     * Tên sự kiện nghiệp vụ — dùng UPPER_SNAKE_CASE.
     * Ví dụ: "BOOKING_CREATED", "PAYMENT_SUCCESS", "HOTEL_APPROVED"
     */
    String event();

    /**
     * Mô tả hành động bằng ngôn ngữ tự nhiên, xuất hiện trong field "message".
     * Ví dụ: "Create a booking", "Process payment", "Approve hotel listing"
     */
    String message();

    /**
     * Log level khi thành công. Mặc định INFO.
     */
    LogLevel successLevel() default LogLevel.INFO;

    /**
     * Log khi bắt đầu method không? Mặc định true.
     */
    boolean logOnEntry() default true;

    /**
     * Đưa giá trị trả về vào log không? Mặc định false.
     */
    boolean logReturnValue() default false;

    enum LogLevel { DEBUG, INFO, WARN }
}
