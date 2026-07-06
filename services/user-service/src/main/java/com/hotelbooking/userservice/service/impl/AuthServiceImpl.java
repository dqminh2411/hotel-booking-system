package com.hotelbooking.userservice.service.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.hotelbooking.userservice.dto.AuthResponse;
import com.hotelbooking.userservice.dto.GoogleLoginRequest;
import com.hotelbooking.userservice.entity.*;
import com.hotelbooking.userservice.repository.AuthProviderRepository;
import com.hotelbooking.userservice.repository.RoleRepository;
import com.hotelbooking.userservice.repository.UserAuthProviderRepository;
import com.hotelbooking.userservice.repository.UserRepository;
import com.hotelbooking.userservice.service.AuthService;
import com.hotelbooking.userservice.service.JwtService;
import com.hotelbooking.userservice.service.TokenHashService;
import com.hotelbooking.userservice.service.UserService;
import jakarta.ws.rs.BadRequestException;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import org.springframework.transaction.annotation.Transactional;

import java.security.AuthProvider;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    @Value("${google.client-id}")
    private String googleClientId;

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthProviderRepository authProviderRepository;
    private final UserAuthProviderRepository userAuthProviderRepository;
    private final TokenHashService tokenHashService;

    @Override
    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {

        GoogleIdToken.Payload payload = verifyGoogleIdToken(request.idToken());

        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String avatarUrl = (String) payload.get("picture");
        String googleId = payload.getSubject();

        UserEntity user = userRepository.findByEmail(payload.getEmail()).orElse(null);
        AuthProviderEntity authProvider = authProviderRepository.findByCode("GOOGLE")
                .orElseThrow(() -> new IllegalStateException("GOOGLE auth provider is not initialized"));
        // if user not exist, create a new user with CUSTOMER role and save the googleId in UserAuthProvider
        if(user == null){
            RoleEntity customerRole = roleRepository.findByNameAndDeletedFalse("CUSTOMER")
                    .orElseThrow(() -> new IllegalStateException("CUSTOMER role is not initialized"));
            Instant now = Instant.now();
            user = new UserEntity();
            user.setId(UUID.randomUUID());
            user.setEmail(email);
            user.setFullName(name.trim());
            user.setStatus(UserStatus.ACTIVE);
            user.setCreatedAt(now);
            user.setUpdatedAt(now);
            user.setDeleted(false);
            user.getRoles().add(customerRole);
            user.setAvatarUrl(avatarUrl);
            user = userRepository.save(user);

            UserAuthProvider userAuthProvider = new UserAuthProvider();
            userAuthProvider.setId(new UserAuthProviderId(user.getId(), authProvider.getCode()));
            userAuthProvider.setUser(user);
            userAuthProvider.setAuthProvider(authProvider);
            userAuthProvider.setProviderUserId(googleId);
            userAuthProvider.setCreatedAt(now);

            userAuthProviderRepository.save(userAuthProvider);

        }
        // if user exist, check if the googleId is already linked to the user
        else{
            user.setFullName(name.trim());
            user.setAvatarUrl(avatarUrl);
            user.setUpdatedAt(Instant.now());
            user = userRepository.save(user);

            UserAuthProvider userAuthProvider = userAuthProviderRepository
                    .findById_UserIdAndId_AuthProviderCode(user.getId(), "GOOGLE")
                    .orElse(null);
            if(userAuthProvider == null){

                UserAuthProvider newUserAuthProvider = new UserAuthProvider();
                newUserAuthProvider.setUser(user);
                newUserAuthProvider.setAuthProvider(authProvider);
                newUserAuthProvider.setProviderUserId(googleId);
                newUserAuthProvider.setCreatedAt(Instant.now());

                userAuthProviderRepository.save(newUserAuthProvider);
            }
        }

        return new AuthResponse(
                jwtService.createAccessToken(user),
                tokenHashService.newOpaqueToken(),
                userService.toResponse(user)
        );
    }

    @Override
    public GoogleIdToken.Payload verifyGoogleIdToken(String idToken) {
        try{
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance()
            )
                    .setAudience(java.util.Collections.singletonList(googleClientId))
                    .build();
            GoogleIdToken googleIdToken = verifier.verify(idToken);

            if(googleIdToken == null){
                throw new BadRequestException("Invalid Google Client ID token");
            }
            GoogleIdToken.Payload payload = googleIdToken.getPayload();
            if(payload==null || !Boolean.TRUE.equals(payload.getEmailVerified())){
                throw new BadRequestException("Email not verified by Google");
            }
            return payload;
        } catch (Exception e) {
            throw new BadRequestException("Failed to verify Google ID token: " + e.getMessage());
        }
    }
}
