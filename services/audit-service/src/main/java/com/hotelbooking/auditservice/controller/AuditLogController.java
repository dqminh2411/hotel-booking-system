package com.hotelbooking.auditservice.controller;

import com.hotelbooking.auditservice.dto.AuditLogRequest;
import com.hotelbooking.auditservice.dto.AuditLogResponse;
import com.hotelbooking.auditservice.dto.AuditLogStatistics;
import com.hotelbooking.auditservice.service.AuditLogService;
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
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogs(AuditLogRequest request) {
        String[] sortParams = request.sort().split(",");
        String sortProperty = sortParams[0];
        Sort.Direction sortDirection = (sortParams.length > 1 && "asc".equalsIgnoreCase(sortParams[1]))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(request.page(), request.size(), Sort.by(sortDirection, sortProperty));

        Page<AuditLogResponse> result = auditLogService.getAuditLogs(request, pageable);

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
