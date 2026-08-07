package com.hotelbooking.chassis.audit;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Map;

/**
 * DTO chứa toàn bộ dữ liệu của một audit log entry.
 * Sẽ được serialize thành JSON bởi AuditLogger.
 */
@Getter
@Builder
@ToString
public class AuditLogEntry {

    /** ID duy nhất của log entry */
    private final String logId;

    /** Thời điểm sự kiện xảy ra */
    private final Instant timestamp;

    /** Loại sự kiện nghiệp vụ */
    private final String eventType;

    /** Mô tả hành động */
    private final String message;

    /** Mức độ nghiêm trọng */
    private final String severity;

    /** ID của người thực hiện hành động */
    private final String actorId;

    /** Loại actor (USER, SYSTEM, SERVICE) */
    private final String actorType;

    /** IP của request */
    private final String requestIp;

    /** ID của đối tượng bị tác động */
    private final String targetId;

    /** Loại đối tượng bị tác động */
    private final String targetType;

    /** Tên service phát sinh sự kiện */
    private final String serviceName;

    /** Trace ID từ OpenTelemetry (nullable nếu không có span) */
    private final String traceId;

    /** Span ID từ OpenTelemetry (nullable nếu không có span) */
    private final String spanId;

    /** HTTP method (GET, POST, PUT, DELETE, ...) */
    private final String httpMethod;

    /** Endpoint được gọi */
    private final String endpoint;

    /** Kết quả: SUCCESS hoặc FAILURE */
    private final String resultStatus;

    /** Thời gian thực thi (milliseconds) */
    private final Long durationMs;

    /** Mã lỗi (nullable khi thành công) */
    private final String errorCode;

    /** Thông điệp lỗi (nullable khi thành công) */
    private final String errorMessage;

    /** Dữ liệu bổ sung dạng key-value */
    private final Map<String, Object> extraData;
}
