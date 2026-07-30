package com.hotelbooking.auditservice.repository;

import com.hotelbooking.auditservice.document.AuditLogDocument;
import com.hotelbooking.auditservice.dto.AuditLogRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AuditLogRepositoryImpl implements AuditLogRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<AuditLogDocument> searchAuditLogs(AuditLogRequest request, Pageable pageable) {
        // Query: Đối tượng bao đóng toàn bộ câu truy vấn MongoDB
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        // Criteria.where("field").is(value): Biểu thức so sánh bằng
        if (StringUtils.hasText(request.tenantId())) {
            criteriaList.add(Criteria.where("tenantId").is(request.tenantId()));
        }
        // Criteria.gte() / lte(): Điều kiện so sánh khoảng thời gian
        if (request.startDate() != null && request.endDate() != null) {
            criteriaList.add(Criteria.where("timestamp").gte(request.startDate()).lte(request.endDate()));
        } else if (request.startDate() != null) {
            criteriaList.add(Criteria.where("timestamp").gte(request.startDate()));
        } else if (request.endDate() != null) {
            criteriaList.add(Criteria.where("timestamp").lte(request.endDate()));
        }
        // Nested document criteria
        if (StringUtils.hasText(request.actorId())) {
            criteriaList.add(Criteria.where("actor.id").is(request.actorId()));
        }
        if (StringUtils.hasText(request.actorType())) {
            criteriaList.add(Criteria.where("actor.type").is(request.actorType()));
        }
        if (StringUtils.hasText(request.action())) {
            criteriaList.add(Criteria.where("action").is(request.action()));
        }
        if (StringUtils.hasText(request.actionCategory())) {
            criteriaList.add(Criteria.where("actionCategory").is(request.actionCategory()));
        }
        if (StringUtils.hasText(request.entityType())) {
            criteriaList.add(Criteria.where("entity.type").is(request.entityType()));
        }
        if (StringUtils.hasText(request.entityId())) {
            criteriaList.add(Criteria.where("entity.id").is(request.entityId()));
        }
        if (StringUtils.hasText(request.severity())) {
            criteriaList.add(Criteria.where("severity").is(request.severity()));
        }
        if (StringUtils.hasText(request.resultStatus())) {
            criteriaList.add(Criteria.where("resultStatus").is(request.resultStatus()));
        }
        if (StringUtils.hasText(request.serviceName())) {
            criteriaList.add(Criteria.where("serviceName").is(request.serviceName()));
        }
        if (StringUtils.hasText(request.correlationId())) {
            criteriaList.add(Criteria.where("correlationId").is(request.correlationId()));
        }
        // TextCriteria & TextQuery: Full-text search
        if (StringUtils.hasText(request.search())) {
            TextCriteria textCriteria = TextCriteria.forDefaultLanguage().matching(request.search());
            query = TextQuery.queryText(textCriteria);
        }

        // Kết hợp điều kiện bằng phép AND ($and)
        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        long total = mongoTemplate.count(query, AuditLogDocument.class);
        query.with(pageable);

        List<AuditLogDocument> documents = mongoTemplate.find(query, AuditLogDocument.class);
        return new PageImpl<>(documents, pageable, total);
    }

    @Override
    public List<AuditLogDocument> findForStatistics(Instant startDate, Instant endDate, String tenantId) {
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

        return mongoTemplate.find(query, AuditLogDocument.class);
    }
}
