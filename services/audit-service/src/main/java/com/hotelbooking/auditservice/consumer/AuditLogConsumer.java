package com.hotelbooking.auditservice.consumer;

import com.hotelbooking.auditservice.dto.AuditEvent;
import com.hotelbooking.auditservice.service.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogConsumer {

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "audit-log-events", groupId = "${spring.kafka.consumer.group-id:audit-service-group}")
    public void consume(String message) {
        log.info("Received raw audit log message from Kafka: {}", message);
        try {
            AuditEvent event = objectMapper.readValue(message, AuditEvent.class);
            auditLogService.processAuditEvent(event);
        } catch (Exception e) {
            log.error("Error processing audit log event message: {}", e.getMessage(), e);
        }
    }
}
