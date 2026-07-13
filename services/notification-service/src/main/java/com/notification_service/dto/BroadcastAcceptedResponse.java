package com.notification_service.dto;

import java.time.Instant;
import java.util.UUID;

public record BroadcastAcceptedResponse(
    UUID broadcastId,
    String status,
    String message,
    Instant acceptedAt
) {
}
