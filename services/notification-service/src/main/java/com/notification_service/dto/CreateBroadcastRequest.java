package com.notification_service.dto;

import com.notification_service.enums.BroadcastSegment;
import com.notification_service.enums.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record CreateBroadcastRequest(
    @NotNull BroadcastSegment segment,
    UUID segmentRefId,
    @NotBlank @Size(max = 255) String title,
    @NotBlank @Size(max = 5000) String body,
    @NotEmpty @Size(max = 2) Set<NotificationChannel> channels,
    Map<String, Object> payload
) {
}
