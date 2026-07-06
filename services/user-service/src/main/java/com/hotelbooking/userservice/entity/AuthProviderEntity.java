package com.hotelbooking.userservice.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "auth_providers")
@Getter
@Setter
public class AuthProviderEntity {
    @Id
    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @NonNull
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @OneToMany(mappedBy = "authProvider")
    private Set<UserAuthProvider> userAuthProviders = new HashSet<>();
}
