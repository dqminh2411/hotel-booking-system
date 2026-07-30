package com.hotelbooking.auditservice.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;

public record AuditLogRequest(
        String tenantId,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
        String actorId,
        String actorType,
        String action,
        String actionCategory,
        String entityType,
        String entityId,
        String severity,
        String resultStatus,
        String serviceName,
        String correlationId,
        String search,
        Integer page,
        Integer size,
        String sort
) {
    public AuditLogRequest {
        if (page == null) page = 0;
        if (size == null) size = 20;
        if (sort == null || sort.isBlank()) sort = "timestamp,desc";
    }
}
