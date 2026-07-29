package com.hotelbooking.auditservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    private String id;
    private String eventId;
    private Instant timestamp;
    private String tenantId;

    private ActorDto actor;

    private String action;
    private String actionCategory;
    private String severity;

    private EntityDto entity;

    private String serviceName;
    private String correlationId;
    private String description;

    private Object oldValue;
    private Object newValue;
    private Object metadata;

    private String resultStatus;
    private String errorMessage;
    private Instant receivedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActorDto {
        private String id;
        private String type;
        private String email;
        private String ip;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EntityDto {
        private String type;
        private String id;
    }
}
