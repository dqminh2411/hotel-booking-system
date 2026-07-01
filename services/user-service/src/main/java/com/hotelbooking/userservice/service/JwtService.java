package com.hotelbooking.userservice.service;

import com.hotelbooking.userservice.entity.UserEntity;
import com.hotelbooking.userservice.exception.ApiException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey key;
    private final Duration accessTokenTtl;
    private final Duration verificationTokenTtl;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-ttl}") Duration accessTokenTtl,
            @Value("${app.auth.verification-token-ttl}") Duration verificationTokenTtl
    ) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtl = accessTokenTtl;
        this.verificationTokenTtl = verificationTokenTtl;
    }

    public String createAccessToken(UserEntity user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("type", "access")
                .claim("email", user.getEmail())
                .claim("roles", user.getRoles().stream().map(role -> role.getName()).sorted().toList())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)))
                .signWith(key)
                .compact();
    }

    public UUID parseUserId(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw unauthorized();
        }
        String token = authorizationHeader.substring(7).trim();
        if (token.isEmpty()) {
            throw unauthorized();
        }
        try {
            var claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
            if (!"access".equals(claims.get("type", String.class))) {
                throw unauthorized();
            }
            return UUID.fromString(claims.getSubject());
        } catch (JwtException | IllegalArgumentException ex) {
            throw unauthorized();
        }
    }

    public String createEmailVerificationToken(UserEntity user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("type", "email-verification")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(verificationTokenTtl)))
                .signWith(key)
                .compact();
    }

    public UUID parseEmailVerificationUserId(String token) {
        try {
            var claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
            if (!"email-verification".equals(claims.get("type", String.class))) {
                throw invalidVerificationToken();
            }
            return UUID.fromString(claims.getSubject());
        } catch (ExpiredJwtException ex) {
            throw new ApiException(HttpStatus.GONE, "TOKEN_EXPIRED", "Verification link has expired");
        } catch (JwtException | IllegalArgumentException ex) {
            throw invalidVerificationToken();
        }
    }

    private ApiException unauthorized() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_ACCESS_TOKEN",
                "Access token is missing, invalid or expired");
    }

    private ApiException invalidVerificationToken() {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TOKEN", "Verification token is invalid");
    }
}
