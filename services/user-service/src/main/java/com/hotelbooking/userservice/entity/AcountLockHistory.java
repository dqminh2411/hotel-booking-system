package com.hotelbooking.userservice.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "account_lock_history")
public class AcountLockHistory {
    @Id
    @Column(name = "id")
    UUID id;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(name = "locked_by", nullable = false)
    UUID lockedBy;

    @Column(name = "reason", nullable = false, length = 500)
    String reason;

    @Column(name = "locked_at", nullable = false)
    Instant lockedAt = Instant.now();

    @Column(name = "unlocked_at", nullable = true)
    Instant unlockedAt;

    @Column(name = "is_deleted", nullable = false)
    Boolean isDeleted = false;
}
