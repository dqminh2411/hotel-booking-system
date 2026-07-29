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
public class AuditEvent {
    private String eventId; // UUID, unique per event — idempotency key
    private Instant timestamp; // Event occurrence time

    private String tenantId; // UUID of tenant

    // Actor
    private String actorId; // UUID, null if SYSTEM
    private String actorType; // CUSTOMER | STAFF | OWNER | ADMIN | SYSTEM
    private String actorEmail; // Snapshot email
    private String actorIp; // IPv4 / IPv6

    // Action
    private String action; // e.g. BOOKING_CREATED, PAYMENT_SUCCESS
    private String actionCategory; // AUTH | BOOKING | PAYMENT | HOTEL | PROMOTION | ADMIN | NOTIFICATION
    private String severity; // INFO | WARN | ERROR

    // Entity
    private String entityType; // BOOKING | HOTEL | USER | PAYMENT | ROOM | PROMOTION
    private String entityId; // ID of target entity

    // Context
    private String serviceName; // booking-service, payment-service, etc.
    private String correlationId; // Trace / saga correlation ID
    private String description; // Human readable description

    // Change Tracking
    private Object oldValue; // Previous value (flexible)
    private Object newValue; // New value (flexible)
    private Object metadata; // Additional metadata

    // Result
    private String resultStatus; // SUCCESS | FAILURE | PARTIAL
    private String errorMessage; // Failure reason if any
}
