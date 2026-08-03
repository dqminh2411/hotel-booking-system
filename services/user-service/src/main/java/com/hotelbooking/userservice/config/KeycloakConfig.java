package com.hotelbooking.userservice.config;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KeycloakConfig {

    @Value("${keycloak.admin.base-url}")
    String adminBaseUrl;

    @Value("${keycloak.admin.realm}")
    String adminRealm;

    @Value("${keycloak.admin.client-id}")
    String adminClientId;

    @Value("${keycloak.admin.client-secret}")
    String adminClientSecret;

    @Bean
    public Keycloak keycloak(){
        return KeycloakBuilder.builder()
                            .serverUrl(adminBaseUrl)
                            .realm(adminRealm)
                            .grantType("client_credentials")
                            .clientId(adminClientId)
                            .clientSecret(adminClientSecret)
                            .build();

    }
}
