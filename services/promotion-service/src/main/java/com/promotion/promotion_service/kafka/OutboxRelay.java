package com.promotion.promotion_service.kafka;

import com.promotion.promotion_service.entity.OutboxEventEntity;
import com.promotion.promotion_service.service.OutboxPublisherService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private final OutboxPublisherService outboxPublisherService;

    // dùng làm fallback cho Event Listener khi outbox event saved DB
    @Scheduled(fixedDelayString = "${outbox.relay.interval-ms:2000}")
    public void relay() {
        List<OutboxEventEntity> events = outboxPublisherService.getAndProcessOutboxEvents();
        outboxPublisherService.sendOutboxEventToKafka(events);
    }
}
