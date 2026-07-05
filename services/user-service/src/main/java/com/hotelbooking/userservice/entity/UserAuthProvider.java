package com.hotelbooking.userservice.entity;


import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(name = "user_auth_providers")
@Data
public class UserAuthProvider {
    @EmbeddedId
    private UserAuthProviderId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("authProviderCode")
    @JoinColumn(name = "auth_provider_code")
    private AuthProviderEntity authProvider;

    @Column(name = "provider_user_id", nullable = false, unique = true)
    private String providerUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;
}
