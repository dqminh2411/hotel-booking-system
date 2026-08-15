package com.hotelbooking.chassis.outbox.service;

import com.hotelbooking.chassis.outbox.entity.OutboxEventEntity;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxRelay {

    private final OutboxPublisherService outboxPublisherService;

    @Scheduled(fixedDelayString = "${outbox.relay.interval-ms:${outbox.publisher.delay:2000}}")
    public void relay() {
        try {
            List<OutboxEventEntity> events = outboxPublisherService.getAndProcessOutboxEvents();
            if (events != null && !events.isEmpty()) {
                outboxPublisherService.sendOutboxEventToKafka(events);
            }
        } catch (Exception ex) {
            log.error("Error running fallback scheduled OutboxRelay", ex);
        }
    }

    public OutboxEventEntity saveEvent(String topic, Object payloadObject, String eventType) {
        return outboxPublisherService.saveOutboxEvent(topic, payloadObject, eventType);
    }

    public OutboxEventEntity saveEvent(String topic, Object payloadObject) {
        return outboxPublisherService.saveOutboxEvent(topic, payloadObject);
    }

    public OutboxEventEntity saveEvent(Object payloadObject) {
        return outboxPublisherService.saveOutboxEvent(payloadObject);
    }

    public OutboxEventEntity saveOutboxMessage(String topic, Object payloadObject, String eventType) {
        return outboxPublisherService.saveOutboxEvent(topic, payloadObject, eventType);
    }
}
