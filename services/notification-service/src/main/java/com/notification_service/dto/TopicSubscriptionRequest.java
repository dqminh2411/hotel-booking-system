package com.notification_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TopicSubscriptionRequest(
    @NotBlank(message = "fcmToken không được để trống")
    @Size(min = 20, max = 500, message = "fcmToken phải từ 20 đến 500 ký tự")
    String fcmToken,

    @NotBlank(message = "topic không được để trống")
    String topic
) {}
