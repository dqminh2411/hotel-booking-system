package com.hotelbooking.auditservice.repository;

import com.hotelbooking.auditservice.document.AuditLogDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuditLogRepository extends MongoRepository<AuditLogDocument, String>, AuditLogRepositoryCustom {

    Optional<AuditLogDocument> findByEventId(String eventId);

    Page<AuditLogDocument> findByEntityTypeAndEntityId(String entityType, String entityId, Pageable pageable);

    boolean existsByEventId(String eventId);
}
