package com.hotelbooking.hotelservice.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelbooking.hotelservice.constant.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class HotelKafkaEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * phase = AFTER_COMMIT: chỉ chạy khi transaction bao quanh
     * HotelServiceImpl.createHotel() đã commit thành công.
     * Nếu transaction rollback (vd lỗi validate ở bước sau), method này
     * sẽ KHÔNG được gọi -> không có message rác trên Kafka.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onHotelCreated(HotelCreatedEvent event) {
        ReviewHotelCommand command = ReviewHotelCommand.builder()
                .eventId(UUID.randomUUID())
                .hotelId(event.hotelId())
                .tenantId(event.tenantId())
                .hotelName(event.hotelName())
                .requestedAt(Instant.now())
                .build();

        try {
            String payload = objectMapper.writeValueAsString(command);

            // Dùng hotelId làm Kafka message key: đảm bảo mọi message liên quan
            // tới cùng 1 hotel luôn rơi vào cùng 1 partition -> giữ đúng thứ tự
            // nếu sau này có thêm các event khác cho cùng hotel (vd HotelApproved).
            kafkaTemplate.send(KafkaTopics.REVIEW_HOTEL_COMMAND, event.hotelId().toString(), payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Gửi ReviewHotelCommand thất bại cho hotelId={}", event.hotelId(), ex);
                        } else {
                            log.info("Đã gửi ReviewHotelCommand cho hotelId={} tới partition={} offset={}",
                                    event.hotelId(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });
        } catch (JsonProcessingException e) {
            // Lỗi serialize không nên xảy ra với DTO đơn giản như thế này,
            // nhưng log rõ để dễ debug nếu sau này thêm field kiểu phức tạp.
            log.error("Không thể serialize ReviewHotelCommand cho hotelId={}", event.hotelId(), e);
        }
    }
}
