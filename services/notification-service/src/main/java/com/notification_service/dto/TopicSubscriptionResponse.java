package com.notification_service.dto;

import java.util.UUID;

public record TopicSubscriptionResponse(
    UUID userId,
    String fcmToken,
    String topic,
    boolean subscribed,
    String message
) {}
