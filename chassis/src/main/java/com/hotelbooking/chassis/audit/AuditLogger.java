package com.hotelbooking.chassis.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Chỉ có nhiệm vụ ghi audit log dưới dạng JSON.
 *
 * Không xử lý business logic.
 * Không xử lý OpenTelemetry.
 * Chỉ serialize AuditLogEntry thành JSON và ghi qua SLF4J logger.
 * 
 */
@Slf4j
public class AuditLogger {

    /**
     * Logger riêng cho audit log — tách biệt khỏi application log.
     * Có thể cấu hình appender riêng trong logback-spring.xml nếu cần.
     */
    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("AUDIT_LOG");

    private final ObjectMapper objectMapper;

    public AuditLogger() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Ghi một dòng JSON audit log.
     *
     * @param entry AuditLogEntry chứa toàn bộ dữ liệu audit
     */
    public void log(AuditLogEntry entry) {
        try {
            String json = objectMapper.writeValueAsString(entry);
            AUDIT_LOG.info(json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize audit log entry: {}", entry, e);
        }
    }
}
