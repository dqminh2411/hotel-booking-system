package com.hotelbooking.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class JwtConfig{
    @Value("${keycloak.jwks-uri}")
    private String jwksUri;
    @Value("${keycloak.issuer-uri}")
    private String issuerUri;

    @Bean

    public ReactiveJwtDecoder jwtDecoder() {
        NimbusReactiveJwtDecoder jwtDecoder = NimbusReactiveJwtDecoder
            .withJwkSetUri(jwksUri)
            .build();

        OAuth2TokenValidator<Jwt> withIssuer =
            JwtValidators.createDefaultWithIssuer(issuerUri);
        jwtDecoder.setJwtValidator(withIssuer);

        return jwtDecoder;
    }
}

