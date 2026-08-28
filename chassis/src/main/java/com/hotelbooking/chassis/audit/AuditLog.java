package com.hotelbooking.chassis.audit;

import java.lang.annotation.*;

/**
 * Annotation đánh dấu method cần audit.
 *
 * Chứa thông tin tĩnh (metadata) và SpEL expression để trích xuất
 * targetId, extraData từ method parameter hoặc return value.
 * 
 *
 * 
 * SpEL context có sẵn:
 * 
 * <ul>
 * <li>Tên parameter: {@code #bookingId}, {@code #command}, ...</li>
 * <li>{@code #result}: giá trị trả về của method (chỉ khi thành công)</li>
 * <li>{@code #args}: mảng tham số method</li>
 * </ul>
 *
 * 
 * Ví dụ:
 * 
 * 
 * <pre>
 * {@code @AuditLog(
 *     eventType = AuditEventType.CREATE_BOOKING,
 *     message = "Create booking",
 *     severity = Severity.INFO,
 *     targetType = TargetType.BOOKING,
 *     targetId = "#command.bookingId",
 *     extraData = "{'hotelId': #command.hotel.hotelId, 'userId': #command.user.userId}"
 * )}
 * public void handleCreateBooking(CreateBookingCommand command) { }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

    /**
     * Loại sự kiện nghiệp vụ.
     */
    AuditEventType eventType();

    /**
     * Mô tả hành động bằng ngôn ngữ tự nhiên.
     */
    String message();

    /**
     * Mức độ nghiêm trọng khi thành công. Mặc định INFO.
     */
    Severity severity() default Severity.INFO;

    /**
     * Loại đối tượng bị tác động.
     */
    TargetType targetType() default TargetType.OTHER;

    /**
     * SpEL expression để trích xuất targetId.
     * Ví dụ: "#bookingId", "#command.bookingId", "#result.id"
     * Để trống nếu không có.
     */
    String targetId() default "";

    /**
     * Danh sách SpEL expression để trích xuất extraData.
     * Mỗi phần tử có format: "key=SpEL_expression"
     * Ví dụ: {"hotelId=#command.hotel.hotelId", "userId=#command.user.userId"}
     */
    String[] extraData() default {};
}
