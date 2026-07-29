package com.hotelbooking.auditservice.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "audit_logs")
@CompoundIndexes({
        @CompoundIndex(name = "idx_tenant_timestamp", def = "{'tenantId': 1, 'timestamp': -1}"),
        @CompoundIndex(name = "idx_actor_timestamp", def = "{'actor.id': 1, 'timestamp': -1}"),
        @CompoundIndex(name = "idx_entity_timestamp", def = "{'entity.type': 1, 'entity.id': 1, 'timestamp': -1}"),
        @CompoundIndex(name = "idx_actionCategory_timestamp", def = "{'actionCategory': 1, 'timestamp': -1}")
})
public class AuditLogDocument {

    @Id
    private String id;

    @Indexed(unique = true, name = "idx_eventId_unique")
    private String eventId;

    private Instant timestamp;
    private String tenantId;

    private Actor actor;

    private String action;
    private String actionCategory;
    private String severity;

    private EntityInfo entity;

    private String serviceName;

    @Indexed(name = "idx_correlationId", sparse = true)
    private String correlationId;

    @TextIndexed(name = "idx_description_text")
    private String description;

    private Object oldValue;
    private Object newValue;
    private Object metadata;

    private String resultStatus;
    private String errorMessage;

    private Instant receivedAt;

    @Indexed(name = "idx_ttl_expiry", expireAfterSeconds = 0)
    private Instant expiresAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Actor {
        private String id;
        private String type;
        private String email;
        private String ip;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EntityInfo {
        private String type;
        private String id;
    }
}
