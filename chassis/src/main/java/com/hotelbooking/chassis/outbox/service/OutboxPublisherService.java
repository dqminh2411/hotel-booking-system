package com.hotelbooking.chassis.outbox.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelbooking.chassis.outbox.entity.OutboxEventEntity;
import com.hotelbooking.chassis.outbox.entity.OutboxEventStatus;
import com.hotelbooking.chassis.outbox.event.OutboxEventPublishEvent;
import com.hotelbooking.chassis.outbox.repository.OutboxEventRepository;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxPublisherService {

    private static final String DEFAULT_TOPIC = "booking-events";
    private static final int MAX_RETRIES = 5;
    private static final int TIMEOUT_IN_SECONDS = 30;

    private final OutboxEventRepository outboxEventRepository;
    @SuppressWarnings("rawtypes")
    private final KafkaTemplate kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    private static final TextMapSetter<Map<String, String>> MAP_SETTER =
        (carrier, key, value) -> carrier.put(key, value);

    private static final TextMapGetter<Map<String, String>> MAP_GETTER =
        new TextMapGetter<>() {
            @Override
            public Iterable<String> keys(Map<String, String> carrier) {
                return carrier != null ? carrier.keySet() : List.of();
            }

            @Override
            public String get(Map<String, String> carrier, String key) {
                return carrier != null ? carrier.get(key) : null;
            }
        };

    public OutboxEventEntity saveOutboxEvent(String topic, Object payloadObject, String eventType) {
        try {
            String payloadStr = payloadObject instanceof String s
                ? s
                : objectMapper.writeValueAsString(payloadObject);

            String traceparent = extractCurrentTraceparent();

            OutboxEventEntity outbox = OutboxEventEntity.builder()
                .id(UUID.randomUUID())
                .topic(topic != null && !topic.isBlank() ? topic : DEFAULT_TOPIC)
                .eventType(eventType != null ? eventType : (payloadObject != null ? payloadObject.getClass().getSimpleName() : "UnknownEvent"))
                .payload(payloadStr)
                .status(OutboxEventStatus.PENDING.name())
                .published(false)
                .retryCount(0)
                .createdAt(Instant.now())
                .isDeleted(false)
                .traceparent(traceparent)
                .build();

            OutboxEventEntity saved = outboxEventRepository.save(outbox);
            eventPublisher.publishEvent(new OutboxEventPublishEvent(saved.getId()));
            return saved;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox payload", e);
        }
    }

    public OutboxEventEntity saveOutboxEvent(String topic, Object payloadObject) {
        String eventType = payloadObject != null ? payloadObject.getClass().getSimpleName() : "UnknownEvent";
        return saveOutboxEvent(topic, payloadObject, eventType);
    }

    public OutboxEventEntity saveOutboxEvent(Object payloadObject) {
        return saveOutboxEvent(DEFAULT_TOPIC, payloadObject);
    }

    @Transactional
    public List<OutboxEventEntity> getAndProcessOutboxEvents() {
        List<OutboxEventEntity> events = outboxEventRepository.findEventsToPublish();
        for (OutboxEventEntity event : events) {
            if (OutboxEventStatus.PENDING.name().equalsIgnoreCase(event.getStatus())) {
                event.setStatus(OutboxEventStatus.PROCESSING.name());
            } else {
                int retries = (event.getRetryCount() != null ? event.getRetryCount() : 0) + 1;
                event.setRetryCount(retries);
                if (retries >= MAX_RETRIES) {
                    event.setStatus(OutboxEventStatus.DEAD_LETTER.name());
                    event.setIsDeleted(true);
                } else {
                    long baseDelay = (long) Math.pow(2, retries);
                    long jitter = ThreadLocalRandom.current().nextLong(baseDelay / 2 + 1);
                    event.setNextRetryAt(Instant.now().plusSeconds(baseDelay + jitter));
                }
            }
            event.setLockedUntil(Instant.now().plusSeconds(TIMEOUT_IN_SECONDS));
        }

        return outboxEventRepository.saveAll(events);
    }

    @SuppressWarnings("unchecked")
    public void sendOutboxEventToKafka(List<OutboxEventEntity> events) {
        for (OutboxEventEntity event : events) {
            if (OutboxEventStatus.DEAD_LETTER.name().equalsIgnoreCase(event.getStatus())) {
                continue;
            }

            try {
                String key = extractKafkaKey(event.getPayload());
                String topic = event.getTopic() != null && !event.getTopic().isBlank()
                    ? event.getTopic()
                    : DEFAULT_TOPIC;

                if (event.getTraceparent() != null && !event.getTraceparent().isBlank()) {
                    Map<String, String> carrier = new HashMap<>();
                    carrier.put("traceparent", event.getTraceparent());

                    Context restoredCtx = GlobalOpenTelemetry.getPropagators()
                        .getTextMapPropagator()
                        .extract(Context.current(), carrier, MAP_GETTER);

                    try (Scope scope = restoredCtx.makeCurrent()) {
                        kafkaTemplate.send(topic, key, event.getPayload()).get();
                    }
                } else {
                    kafkaTemplate.send(topic, key, event.getPayload()).get();
                }

                event.setStatus(OutboxEventStatus.PUBLISHED.name());
                event.setPublished(true);
                event.setPublishedAt(Instant.now());
                outboxEventRepository.save(event);

            } catch (Exception ex) {
                log.error("Failed to relay outbox event id={}", event.getId(), ex);
                int retries = (event.getRetryCount() != null ? event.getRetryCount() : 0) + 1;
                event.setRetryCount(retries);
                if (retries >= MAX_RETRIES) {
                    event.setStatus(OutboxEventStatus.DEAD_LETTER.name());
                    event.setIsDeleted(true);
                }
                outboxEventRepository.save(event);
            }
        }
    }

    private String extractKafkaKey(String payload) {
        if (payload == null || payload.isBlank()) return null;
        try {
            JsonNode node = objectMapper.readTree(payload);
            String key = node.path("bookingId").asText(null);
            if (key == null || key.isBlank()) {
                key = node.path("booking").path("bookingId").asText(null);
            }
            if (key == null || key.isBlank()) {
                key = node.path("sagaId").asText(null);
            }
            if (key == null || key.isBlank()) {
                key = node.path("hotelId").asText(null);
            }
            if (key == null || key.isBlank()) {
                key = node.path("userId").asText(null);
            }
            if (key == null || key.isBlank()) {
                key = node.path("id").asText(null);
            }
            return key;
        } catch (Exception e) {
            return null;
        }
    }

    private String extractCurrentTraceparent() {
        SpanContext ctx = Span.current().getSpanContext();
        if (!ctx.isValid()) return null;

        Map<String, String> carrier = new HashMap<>();
        GlobalOpenTelemetry.getPropagators()
            .getTextMapPropagator()
            .inject(Context.current(), carrier, MAP_SETTER);
        return carrier.get("traceparent");
    }
}
