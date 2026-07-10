package com.notification_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notification_service.dto.EmailRequest;
import com.notification_service.dto.EmailTemplate;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import com.notification_service.enums.KafkaEventType;

@Service
public class KafkaNotificationConsumerService {

    private static final Logger log =
        LoggerFactory.getLogger(KafkaNotificationConsumerService.class);

    private final EmailService emailService;
    private final TemplateService templateService;
    private final FcmPushService fcmPushService;
    private final ObjectMapper objectMapper;

    public KafkaNotificationConsumerService(
        EmailService emailService,
        TemplateService templateService,
        FcmPushService fcmPushService,
        ObjectMapper objectMapper
    ) {
        this.emailService = emailService;
        this.templateService = templateService;
        this.fcmPushService = fcmPushService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "notification-commands", groupId = "notification-service-group")
    public void consume(String payloadJson) throws JsonProcessingException {
        EmailRequest command = objectMapper.readValue(payloadJson, EmailRequest.class);

        try {
            String content = templateService.buildContent(
                EmailTemplate.valueOf(command.eventType()),
                command
            );
            emailService.send(command.to(), "Notification", content);
        } catch (Exception error) {
            log.error("Could not send booking email for booking {}", command.bookingId(), error);
        }

        sendPush(command);
    }

    private void sendPush(EmailRequest command) {
        UUID userId = command.booking() == null || command.booking().customer() == null
            ? null
            : command.booking().customer().userId();
        if (userId == null) {
            log.warn("Notification command has no recipient userId for booking {}", command.bookingId());
            return;
        }

        try {
            boolean confirmed = KafkaEventType.SEND_BOOKING_CONFIRMED.getEventType().equals(command.eventType());
            String status = confirmed ? "CONFIRMED" : "FAILED";
            String title = confirmed ? "Đặt phòng thành công" : "Đặt phòng thất bại";
            String body = confirmed
                ? "Booking " + command.bookingId() + " đã được xác nhận."
                : "Booking " + command.bookingId() + " không thành công."
                    + (command.reason() == null ? "" : " Lý do: " + command.reason());

            fcmPushService.sendBookingUpdate(
                userId,
                title,
                body,
                Map.of(
                    "bookingId", command.bookingId().toString(),
                    "eventType", command.eventType(),
                    "status", status
                )
            );
        } catch (RuntimeException error) {
            log.error("Could not send booking push for user {}", userId, error);
        }
    }
}
