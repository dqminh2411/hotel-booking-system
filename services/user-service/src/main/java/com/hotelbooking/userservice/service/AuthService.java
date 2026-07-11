package com.hotelbooking.userservice.service;

import com.hotelbooking.userservice.dto.AuthResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.hotelbooking.userservice.dto.GoogleLoginRequest;

public interface AuthService {
    AuthResponse loginWithGoogle(GoogleLoginRequest request);
    GoogleIdToken.Payload verifyGoogleIdToken(String idToken);
}
