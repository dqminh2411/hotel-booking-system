package com.notification_service.controller;

import com.notification_service.dto.BroadcastAcceptedResponse;
import com.notification_service.dto.CreateBroadcastRequest;
import com.notification_service.dto.DeviceTokenResponse;
import com.notification_service.dto.NotificationPageResponse;
import com.notification_service.dto.NotificationResponse;
import com.notification_service.dto.RevokeDeviceTokenRequest;
import com.notification_service.dto.TopicSubscriptionRequest;
import com.notification_service.dto.TopicSubscriptionResponse;
import com.notification_service.dto.UpsertDeviceTokenRequest;
import com.notification_service.enums.NotificationEventType;
import com.notification_service.service.NotificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Validated
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    private UUID getUserIdFromJwt(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authentication token");
        }
        return UUID.fromString(jwt.getSubject());
    }

    @PostMapping("/device-token")
    public ResponseEntity<DeviceTokenResponse> upsertDeviceToken(
        @Valid @RequestBody UpsertDeviceTokenRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = getUserIdFromJwt(jwt);
        return ResponseEntity.ok(notificationService.upsertDeviceToken(userId, request));
    }

    @PostMapping("/device-token/revoke")
    public ResponseEntity<Void> revokeDeviceToken(
        @Valid @RequestBody RevokeDeviceTokenRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = getUserIdFromJwt(jwt);
        notificationService.revokeDeviceToken(userId, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/topics/subscribe")
    public ResponseEntity<TopicSubscriptionResponse> subscribeTopic(
        @Valid @RequestBody TopicSubscriptionRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = getUserIdFromJwt(jwt);
        return ResponseEntity.ok(notificationService.subscribeTopic(userId, request));
    }

    @PostMapping("/topics/unsubscribe")
    public ResponseEntity<TopicSubscriptionResponse> unsubscribeTopic(
        @Valid @RequestBody TopicSubscriptionRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = getUserIdFromJwt(jwt);
        return ResponseEntity.ok(notificationService.unsubscribeTopic(userId, request));
    }

    @GetMapping("/my")
    public ResponseEntity<NotificationPageResponse> listMyNotifications(
        @RequestParam(defaultValue = "false") boolean unreadOnly,
        @RequestParam(required = false) NotificationEventType eventType,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
        @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = getUserIdFromJwt(jwt);
        return ResponseEntity.ok(notificationService.listNotifications(userId, unreadOnly, eventType, page, size));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markNotificationRead(
        @PathVariable UUID notificationId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = getUserIdFromJwt(jwt);
        return ResponseEntity.ok(notificationService.markAsRead(notificationId, userId));
    }

    @PostMapping("/broadcast")
    public ResponseEntity<BroadcastAcceptedResponse> createBroadcast(
        @Valid @RequestBody CreateBroadcastRequest request,
        @RequestAttribute(value = "role", required = false) String role,
        @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = getUserIdFromJwt(jwt);
        return ResponseEntity.ok(notificationService.validateAndInitiateBroadcast(request, userId, role));
    }
}
