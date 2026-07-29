package com.hotelbooking.auditservice.controller;

import com.audit_service.dto.AuditLogResponse;
import com.audit_service.dto.AuditLogStatistics;
import com.audit_service.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogs(
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) String actorType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String actionCategory,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String resultStatus,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String correlationId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "timestamp,desc") String sort) {

        String[] sortParams = sort.split(",");
        String sortProperty = sortParams[0];
        Sort.Direction sortDirection = (sortParams.length > 1 && "asc".equalsIgnoreCase(sortParams[1]))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortProperty));

        Page<AuditLogResponse> result = auditLogService.getAuditLogs(
                tenantId, startDate, endDate, actorId, actorType, action,
                actionCategory, entityType, entityId, severity, resultStatus,
                serviceName, correlationId, search, pageable);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<AuditLogResponse> getAuditLogByEventId(@PathVariable String eventId) {
        return auditLogService.getAuditLogByEventId(eventId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/entity/{entityType}/{entityId}/history")
    public ResponseEntity<Page<AuditLogResponse>> getEntityHistory(
            @PathVariable String entityType,
            @PathVariable String entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditLogResponse> result = auditLogService.getEntityHistory(entityType, entityId, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/statistics")
    public ResponseEntity<AuditLogStatistics> getStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(required = false) String tenantId) {

        AuditLogStatistics stats = auditLogService.getStatistics(startDate, endDate, tenantId);
        return ResponseEntity.ok(stats);
    }
}
