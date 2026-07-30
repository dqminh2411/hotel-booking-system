package com.hotelbooking.auditservice.repository;

import com.hotelbooking.auditservice.document.AuditLogDocument;
import com.hotelbooking.auditservice.dto.AuditLogRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

public interface AuditLogRepositoryCustom {

    Page<AuditLogDocument> searchAuditLogs(AuditLogRequest request, Pageable pageable);

    List<AuditLogDocument> findForStatistics(Instant startDate, Instant endDate, String tenantId);
}
