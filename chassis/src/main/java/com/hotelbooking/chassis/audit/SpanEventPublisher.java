package com.hotelbooking.chassis.audit;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Chỉ có nhiệm vụ thêm Span Event lên OpenTelemetry span hiện tại.
 *
 * Nếu span không tồn tại hoặc không recording → bỏ qua, không throw exception.
 * 
 *
 * 
 * Chỉ thêm các trường có ý nghĩa nghiệp vụ vào span event.
 * Các thông tin như timestamp, traceId, spanId, serviceName, endpoint,
 * duration, httpMethod
 * đã có ở cấp Span nên không lặp lại.
 * 
 */
@Slf4j
public class SpanEventPublisher {

    /**
     * Thêm span event cho trường hợp thành công.
     */
    public void publish(AuditLogEntry entry) {
        try {
            Span span = Span.current();
            if (!span.isRecording()) {
                return;
            }

            AttributesBuilder builder = Attributes.builder();
            putIfNotNull(builder, "audit.eventType", entry.getEventType());
            putIfNotNull(builder, "audit.message", entry.getMessage());
            putIfNotNull(builder, "audit.severity", entry.getSeverity());
            putIfNotNull(builder, "audit.actorId", entry.getActorId());
            putIfNotNull(builder, "audit.actorType", entry.getActorType());
            putIfNotNull(builder, "audit.targetId", entry.getTargetId());
            putIfNotNull(builder, "audit.targetType", entry.getTargetType());
            putIfNotNull(builder, "audit.resultStatus", entry.getResultStatus());
            putIfNotNull(builder, "audit.errorCode", entry.getErrorCode());
            putIfNotNull(builder, "audit.errorMessage", entry.getErrorMessage());

            // Thêm metadata nếu có
            if (entry.getExtraData() != null && !entry.getExtraData().isEmpty()) {
                for (Map.Entry<String, Object> meta : entry.getExtraData().entrySet()) {
                    if (meta.getValue() != null) {
                        builder.put("audit.metadata." + meta.getKey(), String.valueOf(meta.getValue()));
                    }
                }
            }

            span.addEvent("audit." + entry.getEventType(), builder.build());

        } catch (Exception e) {
            // Không để việc tạo Span Event làm ảnh hưởng request
            log.warn("Failed to publish span event for audit: {}", entry.getEventType(), e);
        }
    }

    /**
     * Ghi exception lên span hiện tại.
     */
    public void recordException(Throwable throwable) {
        try {
            Span span = Span.current();
            if (!span.isRecording()) {
                return;
            }
            span.recordException(throwable);
            span.setStatus(StatusCode.ERROR, throwable.getMessage());
        } catch (Exception e) {
            log.warn("Failed to record exception on span", e);
        }
    }

    private void putIfNotNull(AttributesBuilder builder, String key, String value) {
        if (value != null) {
            builder.put(key, value);
        }
    }
}
