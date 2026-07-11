package com.hotelbooking.userservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.kafka.common.record.UnalignedMemoryRecords;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@AllArgsConstructor
@NoArgsConstructor
public class UserAuthProviderId implements Serializable {
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "auth_provider_code")
    private String authProviderCode;
}
