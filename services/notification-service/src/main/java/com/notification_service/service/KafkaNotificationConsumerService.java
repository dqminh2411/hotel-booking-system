package com.notification_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notification_service.dto.EmailHotelStatusRequest;
import com.notification_service.dto.EmailHotelStatusTemplate;
import com.notification_service.dto.EmailRequest;
import com.notification_service.dto.EmailTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
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
        ObjectMapper objectMapper, EmailRequest emailRequest
    ) {
        this.emailService = emailService;
        this.templateService = templateService;
        this.fcmPushService = fcmPushService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "notification-commands", groupId = "notification-service-group")
    public void consume(String payloadJson) throws JsonProcessingException {
        EmailRequest command = objectMapper.readValue(payloadJson, EmailRequest.class);

        if (command.to() != null && !command.to().isBlank()) {
            try {
                String content = templateService.buildContent(
                    EmailTemplate.valueOf(command.eventType()),
                    command
                );
                emailService.send(command.to(), "Notification", content);
            } catch (Exception error) {
                log.error("Could not send booking email for booking {}", command.bookingId(), error);
            }
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

    @KafkaListener(
        topics = {"promotion-active-notification", "coupon-active-notification"},
        groupId = "notification-service-group"
    )
    public void consumePromotionNotification(
        String payloadJson,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic
    ) {
        log.info("Received active notification event on topic {}: {}", topic, payloadJson);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> event = objectMapper.readValue(payloadJson, Map.class);
            
            String title;
            String body;
            Map<String, String> data = new HashMap<>();

            for (Map.Entry<String, Object> entry : event.entrySet()) {
                if (entry.getValue() != null) {
                    data.put(entry.getKey(), entry.getValue().toString());
                }
            }

            if ("coupon-active-notification".equals(topic)) {
                String code = (String) event.getOrDefault("code", "");
                String promotionName = (String) event.getOrDefault("promotionName", "Khuyến mãi");
                String discountValue = String.valueOf(event.getOrDefault("discountValue", ""));
                
                title = "Mã giảm giá mới: " + code;
                body = String.format("Nhập mã '%s' để nhận ưu đãi %s cho chương trình '%s'.", code, discountValue, promotionName);
            } else {
                String name = (String) event.getOrDefault("name", "Khuyến mãi mới");
                String description = (String) event.getOrDefault("description", "Vào ngay app để nhận ưu đãi đặt phòng!");
                
                title = "Khuyến mãi mới: " + name;
                body = description.isBlank() ? "Vào ngay ứng dụng để xem chi tiết ưu đãi mới!" : description;
            }

            String targetFcmTopic = resolveFcmTopic(topic, event);
            log.info("Dispatching FCM push notification to topic '{}' with title: '{}'", targetFcmTopic, title);
            
            fcmPushService.sendTopicNotification(targetFcmTopic, title, body, data);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse promotion/coupon notification event from topic {}", topic, e);
        } catch (Exception e) {
            log.error("Error processing active notification event on topic {}", topic, e);
        }
    }

    @KafkaListener(
        topics = {"hotel-status-actions"},
        groupId = "notification-service-group"
    )
    public void consumeHotelUpdateStatusNotification(String payloadJson) throws JsonProcessingException {
        EmailHotelStatusRequest request = objectMapper.readValue(payloadJson, EmailHotelStatusRequest.class);
        String emailTenant = request.tenant().email();
        if(emailTenant != null && !emailTenant.isBlank()){
            try {
                String content = templateService.buildHotelEmailContent(
                    EmailHotelStatusTemplate.valueOf(request.eventType()),
                    request);
                emailService.send(request.tenant().email(),
                                "Thông báo về kiểm duyệt đăng thông tin khách sạn",
                                content);   
            } catch (Exception e) {
                log.error("Could not send notification email for tenant {}", request.tenant().name(), e);
            }
        }
    }
    
    public String resolveFcmTopic(String kafkaTopic, Map<String, Object> event) {
        String scopeType = (String) event.get("scopeType");
        String scopeRefId = (String) event.get("scopeRefId");

        if ("HOTEL".equalsIgnoreCase(scopeType) && scopeRefId != null && !scopeRefId.isBlank()) {
            return kafkaTopic + "-hotel-" + scopeRefId;
        } else if ("USER_SEGMENT".equalsIgnoreCase(scopeType) && scopeRefId != null && !scopeRefId.isBlank()) {
            return kafkaTopic + "-segment-" + scopeRefId;
        }

        
        return kafkaTopic;
    }
}
