package com.promotion.promotion_service.service.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelbooking.chassis.outbox.service.OutboxRelay;
import com.promotion.promotion_service.dto.kafka.ConfirmPromotionUsageCommand;
import com.promotion.promotion_service.dto.kafka.PromotionRejectedEvent;
import com.promotion.promotion_service.dto.kafka.ReleasePromotionUsageCommand;
import com.promotion.promotion_service.dto.kafka.ValidatePromotionCommand;
import com.promotion.promotion_service.service.PromotionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PromotionKafkaConsumerService {

    PromotionService promotionService;
    RedissonClient redissonClient;
    OutboxRelay outboxPublisherService;
    ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "promotion-commands", groupId = "${spring.kafka.consumer.group-id:promotion-service}")
    @Transactional
    public void promotionCommandsHandler(String payloadJson) {
        try {
            Map<String, Object> payload = objectMapper.readValue(payloadJson, Map.class);
            String eventType = (String) payload.get("eventType");
            log.info("Received promotion command eventType: {}", eventType);

            switch (eventType) {
                case "ValidatePromotion" -> {
                    ValidatePromotionCommand command = objectMapper.convertValue(payload, ValidatePromotionCommand.class);
                    handleLockValidatePromotion(command);
                }
                case "ConfirmPromotionUsage" -> {
                    ConfirmPromotionUsageCommand command = objectMapper.convertValue(payload, ConfirmPromotionUsageCommand.class);
                    promotionService.handleConfirmPromotionUsage(command);
                }
                case "ReleasePromotionUsage" -> {
                    ReleasePromotionUsageCommand command = objectMapper.convertValue(payload, ReleasePromotionUsageCommand.class);
                    promotionService.handleReleasePromotionUsage(command);
                }
                default -> log.warn("Unknown eventType in promotion-commands: {}", eventType);
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse promotion command payload: {}", payloadJson, e);
        }
    }

    /**
     * Hàm thực hiện xin khóa Redis (Distributed Lock) trước khi kiểm tra và giữ lượt dùng Khuyến mãi/Coupon.
     * Tránh tình trạng Race Condition (nhiều giao dịch cùng lúc dùng vượt quá số lượt giới hạn của Coupon/Promotion).
     *
     *  ValidatePromotion chứa thông tin đặt phòng, promotionId và couponCode
     */
    private void handleLockValidatePromotion(ValidatePromotionCommand command) {
        // Tên khóa trong Redis:
        // - "lock:promotion:{promotionId}:{couponCode}" nếu dùng coupon giảm giá
        // - "lock:promotion:{promotionId}" nếu chỉ dùng promotion giảm giá trực tiếp
        String lockKey;
        if (command.getCouponCode() != null && !command.getCouponCode().isBlank()) {
            if (command.getPromotionId() != null) {
                lockKey = "lock:promotion:" + command.getPromotionId() + ":" + command.getCouponCode();
            } else {
                lockKey = "lock:promotion:" + command.getCouponCode();
            }
        } else if (command.getPromotionId() != null) {
            lockKey = "lock:promotion:" + command.getPromotionId();
        } else {
            lockKey = "lock:promotion:" + command.getBookingId();
        }

        RLock lock = redissonClient.getLock(lockKey);
        boolean isLocked = false;
        try {
            // Đặt thời gian xin khóa Redis:
            // - waitTime = 5: Đợi xin khóa tối đa 5 giây (nếu sau 5s không lấy được khóa -> thất bại/bận)
            // - leaseTime = -1: Kích hoạt cơ chế tự động gia hạn khóa (Watchdog của Redisson) cho đến khi xử lý xong
            // trong thực tế, dự định để leaseTime = 15s
            isLocked = lock.tryLock(5, -1, TimeUnit.SECONDS);

            if (!isLocked) {
                log.warn("Không thể lấy khóa Redis cho promotion/coupon (Key: {}). Hệ thống đang bận.", lockKey);
                PromotionRejectedEvent rejectedEvent = PromotionRejectedEvent.builder()
                        .sagaId(command.getSagaId())
                        .bookingId(command.getBookingId())
                        .reason("Hệ thống đang bận xử lý mã giảm giá, vui lòng thử lại sau")
                        .build();
                outboxPublisherService.saveOutboxMessage("promotion-events", rejectedEvent, "PromotionRejected");
                return;
            }

            log.info("Lấy khóa Redis thành công cho Key: {}", lockKey);
            // Sau khi lấy khóa thành công mới gọi hàm xử lý nghiệp vụ validate & reserve lượt dùng
            promotionService.handleValidatePromotion(command);

        } catch (InterruptedException ex) {
            // Xử lý khi luồng bị ngắt trong lúc chờ xin khóa
            Thread.currentThread().interrupt();
            log.error("Tiến trình chờ khóa Redis bị ngắt (Key: {})", lockKey, ex);
            throw new RuntimeException("Lỗi máy chủ khi đang xử lý khóa mã giảm giá", ex);
        } catch (Exception e) {
            log.error("Lỗi khi xử lý validate promotion với Redis lock (Key: {}): {}", lockKey, e.getMessage(), e);
            throw e;
        } finally {
            // Giải phóng khóa Redis an toàn sau khi đã hoàn tất xử lý
            if (isLocked) {
                try {
                    lock.unlock();
                } catch (Exception e) {
                    log.error("Lỗi khi giải phóng khóa Redis (Key: {}). Khóa sẽ tự động hết hạn.", lockKey, e);
                }
            }
        }
    }
}
