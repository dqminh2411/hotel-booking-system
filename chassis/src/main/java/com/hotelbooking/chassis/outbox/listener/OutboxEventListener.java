package com.hotelbooking.chassis.outbox.listener;

import com.hotelbooking.chassis.outbox.entity.OutboxEventEntity;
import com.hotelbooking.chassis.outbox.event.OutboxEventPublishEvent;
import com.hotelbooking.chassis.outbox.service.OutboxPublisherService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventListener {

    private final OutboxPublisherService outboxPublisherService;

    @Async("outboxEventPublishExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOutboxSaved(OutboxEventPublishEvent event) {
        try {
            List<OutboxEventEntity> events = outboxPublisherService.getAndProcessOutboxEvents();
            if (events != null && !events.isEmpty()) {
                outboxPublisherService.sendOutboxEventToKafka(events);
            }
        } catch (Exception ex) {
            log.error("Error processing outbox events in @TransactionalEventListener", ex);
        }
    }
}
