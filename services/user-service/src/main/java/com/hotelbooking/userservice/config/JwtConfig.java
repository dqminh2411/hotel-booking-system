package com.hotelbooking.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class JwtConfig{
    @Value("${keycloak.jwks-uri}")
    private String jwksUri;
    @Value("${keycloak.issuer-uri}")
    private String issuerUri;

    @Bean

    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder
            .withJwkSetUri(jwksUri)
            .build();

        OAuth2TokenValidator<Jwt> withIssuer =
            JwtValidators.createDefaultWithIssuer(issuerUri);
        jwtDecoder.setJwtValidator(withIssuer);

        return jwtDecoder;
    }
}
