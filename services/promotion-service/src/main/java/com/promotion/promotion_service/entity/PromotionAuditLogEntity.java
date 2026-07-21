package com.promotion.promotion_service.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.promotion.promotion_service.constant.audit.PromotionAuditAction;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "promotion_audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PromotionAuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    @ToString.Include
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false)
    PromotionEntity promotion;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    PromotionAuditAction action;

    @Column(name = "actor_id")
    UUID actorId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_value")
    JsonNode oldValue;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_value")
    JsonNode newValue;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    OffsetDateTime createdAt;
}