package com.notification_service.dto;

import java.util.List;

public record NotificationPageResponse(
    List<NotificationResponse> content,
    Integer page,
    Integer size,
    Long totalElements,
    Integer totalPages,
    Long unreadCount
) {
}
