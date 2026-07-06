package com.notification_service.controller;

import com.notification_service.dto.BroadcastAcceptedResponse;
import com.notification_service.dto.CreateBroadcastRequest;
import com.notification_service.dto.DeviceTokenResponse;
import com.notification_service.dto.NotificationPageResponse;
import com.notification_service.dto.NotificationResponse;
import com.notification_service.dto.UpsertDeviceTokenRequest;
import com.notification_service.enums.NotificationEventType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
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

@Validated
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @PostMapping("/device-token")
    public ResponseEntity<DeviceTokenResponse> upsertDeviceToken(
        @Valid @RequestBody UpsertDeviceTokenRequest request,
        @RequestAttribute("userId") UUID userId
    ) {
        // TODO
        return null;
    }

    @GetMapping("/my")
    public ResponseEntity<NotificationPageResponse> listMyNotifications(
        @RequestAttribute("userId") UUID userId,
        @RequestParam(defaultValue = "false") boolean unreadOnly,
        @RequestParam(required = false) NotificationEventType eventType,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        // TODO
        return null;
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markNotificationRead(
        @PathVariable UUID notificationId,
        @RequestAttribute("userId") UUID userId
    ) {
        // TODO
        return null;
    }

    @PostMapping("/broadcast")
    public ResponseEntity<BroadcastAcceptedResponse> createBroadcast(
        @Valid @RequestBody CreateBroadcastRequest request,
        @RequestAttribute("userId") UUID userId,
        @RequestAttribute("role") String role
    ) {
        // TODO
        return null;
    }
}
