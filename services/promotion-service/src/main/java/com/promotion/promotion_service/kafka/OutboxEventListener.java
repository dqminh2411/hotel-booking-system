package com.promotion.promotion_service.kafka;

import java.util.List;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.promotion.promotion_service.dto.OutboxEventPublishEvent;
import com.promotion.promotion_service.entity.OutboxEventEntity;
import com.promotion.promotion_service.service.OutboxPublisherService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventListener {

    private final OutboxPublisherService outboxPublisherService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOutboxSaved(OutboxEventPublishEvent event) {
        List<OutboxEventEntity> events = outboxPublisherService.getAndProcessOutboxEvents();
        outboxPublisherService.sendOutboxEventToKafka(events);
    }
}
