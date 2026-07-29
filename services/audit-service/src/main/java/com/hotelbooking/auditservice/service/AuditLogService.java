package com.hotelbooking.auditservice.service;

import com.audit_service.document.AuditLogDocument;
import com.audit_service.dto.AuditEvent;
import com.audit_service.dto.AuditLogResponse;
import com.audit_service.dto.AuditLogStatistics;
import com.audit_service.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final MongoTemplate mongoTemplate;

    public void processAuditEvent(AuditEvent event) {
        if (event == null || !StringUtils.hasText(event.getEventId())) {
            log.warn("Received invalid AuditEvent, skipping: {}", event);
            return;
        }

        Instant eventTimestamp = event.getTimestamp() != null ? event.getTimestamp() : Instant.now();
        Instant expiresAt = eventTimestamp.plus(365, ChronoUnit.DAYS); // 12-month retention

        AuditLogDocument document = AuditLogDocument.builder()
                .eventId(event.getEventId())
                .timestamp(eventTimestamp)
                .tenantId(event.getTenantId())
                .actor(AuditLogDocument.Actor.builder()
                        .id(event.getActorId())
                        .type(event.getActorType())
                        .email(event.getActorEmail())
                        .ip(event.getActorIp())
                        .build())
                .action(event.getAction())
                .actionCategory(event.getActionCategory())
                .severity(event.getSeverity())
                .entity(AuditLogDocument.EntityInfo.builder()
                        .type(event.getEntityType())
                        .id(event.getEntityId())
                        .build())
                .serviceName(event.getServiceName())
                .correlationId(event.getCorrelationId())
                .description(event.getDescription())
                .oldValue(event.getOldValue())
                .newValue(event.getNewValue())
                .metadata(event.getMetadata())
                .resultStatus(event.getResultStatus() != null ? event.getResultStatus() : "SUCCESS")
                .errorMessage(event.getErrorMessage())
                .receivedAt(Instant.now())
                .expiresAt(expiresAt)
                .build();

        try {
            auditLogRepository.insert(document);
            log.info("Successfully persisted AuditEvent id={} action={}", event.getEventId(), event.getAction());
        } catch (DuplicateKeyException e) {
            log.debug("Duplicate AuditEvent skipped: eventId={}", event.getEventId());
        } catch (Exception e) {
            log.error("Failed to insert AuditEvent id={}: {}", event.getEventId(), e.getMessage(), e);
        }
    }

    public Page<AuditLogResponse> getAuditLogs(
            String tenantId,
            Instant startDate,
            Instant endDate,
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
            Pageable pageable) {

        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        if (StringUtils.hasText(tenantId)) {
            criteriaList.add(Criteria.where("tenantId").is(tenantId));
        }
        if (startDate != null && endDate != null) {
            criteriaList.add(Criteria.where("timestamp").gte(startDate).lte(endDate));
        } else if (startDate != null) {
            criteriaList.add(Criteria.where("timestamp").gte(startDate));
        } else if (endDate != null) {
            criteriaList.add(Criteria.where("timestamp").lte(endDate));
        }
        if (StringUtils.hasText(actorId)) {
            criteriaList.add(Criteria.where("actor.id").is(actorId));
        }
        if (StringUtils.hasText(actorType)) {
            criteriaList.add(Criteria.where("actor.type").is(actorType));
        }
        if (StringUtils.hasText(action)) {
            criteriaList.add(Criteria.where("action").is(action));
        }
        if (StringUtils.hasText(actionCategory)) {
            criteriaList.add(Criteria.where("actionCategory").is(actionCategory));
        }
        if (StringUtils.hasText(entityType)) {
            criteriaList.add(Criteria.where("entity.type").is(entityType));
        }
        if (StringUtils.hasText(entityId)) {
            criteriaList.add(Criteria.where("entity.id").is(entityId));
        }
        if (StringUtils.hasText(severity)) {
            criteriaList.add(Criteria.where("severity").is(severity));
        }
        if (StringUtils.hasText(resultStatus)) {
            criteriaList.add(Criteria.where("resultStatus").is(resultStatus));
        }
        if (StringUtils.hasText(serviceName)) {
            criteriaList.add(Criteria.where("serviceName").is(serviceName));
        }
        if (StringUtils.hasText(correlationId)) {
            criteriaList.add(Criteria.where("correlationId").is(correlationId));
        }
        if (StringUtils.hasText(search)) {
            TextCriteria textCriteria = TextCriteria.forDefaultLanguage().matching(search);
            query = TextQuery.queryText(textCriteria);
        }

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        long total = mongoTemplate.count(query, AuditLogDocument.class);
        query.with(pageable);

        List<AuditLogDocument> documents = mongoTemplate.find(query, AuditLogDocument.class);
        List<AuditLogResponse> responses = documents.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, total);
    }

    public Optional<AuditLogResponse> getAuditLogByEventId(String eventId) {
        return auditLogRepository.findByEventId(eventId).map(this::mapToResponse);
    }

    public Page<AuditLogResponse> getEntityHistory(String entityType, String entityId, Pageable pageable) {
        Page<AuditLogDocument> page = auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable);
        return page.map(this::mapToResponse);
    }

    public AuditLogStatistics getStatistics(Instant startDate, Instant endDate, String tenantId) {
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        if (StringUtils.hasText(tenantId)) {
            criteriaList.add(Criteria.where("tenantId").is(tenantId));
        }
        if (startDate != null && endDate != null) {
            criteriaList.add(Criteria.where("timestamp").gte(startDate).lte(endDate));
        }

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        List<AuditLogDocument> documents = mongoTemplate.find(query, AuditLogDocument.class);

        long totalEvents = documents.size();

        Map<String, Long> byCategoryMap = documents.stream()
                .filter(d -> d.getActionCategory() != null)
                .collect(Collectors.groupingBy(AuditLogDocument::getActionCategory, Collectors.counting()));

        List<AuditLogStatistics.CategoryCount> categoryCounts = byCategoryMap.entrySet().stream()
                .map(e -> new AuditLogStatistics.CategoryCount(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        Map<String, Long> bySeverityMap = documents.stream()
                .filter(d -> d.getSeverity() != null)
                .collect(Collectors.groupingBy(AuditLogDocument::getSeverity, Collectors.counting()));

        List<AuditLogStatistics.SeverityCount> severityCounts = bySeverityMap.entrySet().stream()
                .map(e -> new AuditLogStatistics.SeverityCount(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        return AuditLogStatistics.builder()
                .totalEvents(totalEvents)
                .byActionCategory(categoryCounts)
                .bySeverity(severityCounts)
                .dailyTrend(List.of())
                .topActors(List.of())
                .build();
    }

    private AuditLogResponse mapToResponse(AuditLogDocument doc) {
        AuditLogResponse.ActorDto actorDto = null;
        if (doc.getActor() != null) {
            actorDto = AuditLogResponse.ActorDto.builder()
                    .id(doc.getActor().getId())
                    .type(doc.getActor().getType())
                    .email(doc.getActor().getEmail())
                    .ip(doc.getActor().getIp())
                    .build();
        }

        AuditLogResponse.EntityDto entityDto = null;
        if (doc.getEntity() != null) {
            entityDto = AuditLogResponse.EntityDto.builder()
                    .type(doc.getEntity().getType())
                    .id(doc.getEntity().getId())
                    .build();
        }

        return AuditLogResponse.builder()
                .id(doc.getId())
                .eventId(doc.getEventId())
                .timestamp(doc.getTimestamp())
                .tenantId(doc.getTenantId())
                .actor(actorDto)
                .action(doc.getAction())
                .actionCategory(doc.getActionCategory())
                .severity(doc.getSeverity())
                .entity(entityDto)
                .serviceName(doc.getServiceName())
                .correlationId(doc.getCorrelationId())
                .description(doc.getDescription())
                .oldValue(doc.getOldValue())
                .newValue(doc.getNewValue())
                .metadata(doc.getMetadata())
                .resultStatus(doc.getResultStatus())
                .errorMessage(doc.getErrorMessage())
                .receivedAt(doc.getReceivedAt())
                .build();
    }
}
