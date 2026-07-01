package com.hotelbooking.userservice.service.impl;

import com.hotelbooking.userservice.dto.*;
import com.hotelbooking.userservice.entity.*;
import com.hotelbooking.userservice.exception.ApiException;
import com.hotelbooking.userservice.exception.UserNotFoundException;
import com.hotelbooking.userservice.repository.*;
import com.hotelbooking.userservice.service.*;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenHashService tokenHashService;
    private final JwtService jwtService;
    private final VerificationEmailService emailService;
    private final Duration refreshTokenTtl;

    public UserServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            TokenHashService tokenHashService,
            JwtService jwtService,
            VerificationEmailService emailService,
            @Value("${app.auth.refresh-token-ttl}") Duration refreshTokenTtl
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenHashService = tokenHashService;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    @Override
    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String phone = request.phone().trim();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS",
                    "Email is already used by another account");
        }
        if (userRepository.existsByPhone(phone)) {
            throw new ApiException(HttpStatus.CONFLICT, "PHONE_ALREADY_EXISTS",
                    "Phone is already used by another account");
        }

        RoleEntity customerRole = roleRepository.findByNameAndDeletedFalse("CUSTOMER")
                .orElseThrow(() -> new IllegalStateException("CUSTOMER role is not initialized"));
        Instant now = Instant.now();
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setStatus(UserStatus.UNVERIFIED);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setDeleted(false);
        user.getRoles().add(customerRole);
        userRepository.save(user);

        String verificationToken = jwtService.createEmailVerificationToken(user);
        emailService.send(user.getEmail(), user.getFullName(), verificationToken);
        return toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse verifyEmail(String token) {
        UUID userId = jwtService.parseEmailVerificationUserId(token);
        UserEntity user = userRepository.findWithRolesByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TOKEN",
                        "Verification token is invalid"));
        Instant now = Instant.now();
        if (user.isDeleted()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TOKEN", "Verification token is invalid");
        }
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new ApiException(HttpStatus.LOCKED, "ACCOUNT_LOCKED", "Account is locked");
        }
        user.setStatus(UserStatus.ACTIVE);
        user.setUpdatedAt(now);
        return toResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findWithRolesByEmailIgnoreCaseAndDeletedFalse(request.email().trim())
                .orElseThrow(this::invalidCredentials);
        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        ensureCanAuthenticate(user);

        String accessToken = jwtService.createAccessToken(user);
        String rawRefreshToken = tokenHashService.newOpaqueToken();
        Instant now = Instant.now();
        RefreshTokenEntity refreshToken = new RefreshTokenEntity();
        refreshToken.setId(UUID.randomUUID());
        refreshToken.setUser(user);
        refreshToken.setTokenHash(tokenHashService.hash(rawRefreshToken));
        refreshToken.setCreatedAt(now);
        refreshToken.setExpiresAt(now.plus(refreshTokenTtl));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);
        return new AuthResponse(accessToken, rawRefreshToken, toResponse(user));
    }

    @Override
    @Transactional(readOnly = true)
    public AccessTokenResponse refreshAccessToken(String refreshToken) {
        RefreshTokenEntity stored = refreshTokenRepository
                .findByTokenHashAndRevokedFalse(tokenHashService.hash(refreshToken))
                .orElseThrow(this::invalidRefreshToken);
        if (!stored.getExpiresAt().isAfter(Instant.now())) {
            throw invalidRefreshToken();
        }
        ensureCanAuthenticate(stored.getUser());
        return new AccessTokenResponse(jwtService.createAccessToken(stored.getUser()));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String authorizationHeader) {
        return getUserById(jwtService.parseUserId(authorizationHeader));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        UserEntity user = userRepository.findWithRolesByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return toResponse(user);
    }

    private void ensureCanAuthenticate(UserEntity user) {
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new ApiException(HttpStatus.LOCKED, "ACCOUNT_LOCKED", "Account is locked");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "EMAIL_NOT_VERIFIED",
                    "Email address has not been verified");
        }
    }

    private ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password");
    }

    private ApiException invalidRefreshToken() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN",
                "Refresh token is invalid, revoked or expired");
    }

    private UserResponse toResponse(UserEntity user) {
        List<String> roles = user.getRoles().stream()
                .map(RoleEntity::getName).distinct().sorted().toList();
        return new UserResponse(
                user.getId(), user.getId(), user.getEmail(), user.getPhone(),
                user.getFullName(), user.getFullName(), user.getAvatarUrl(), user.getAddress(),
                user.getStatus().name(), user.getGoogleId(), roles, user.getCreatedAt(), user.getUpdatedAt()
        );
    }
}
