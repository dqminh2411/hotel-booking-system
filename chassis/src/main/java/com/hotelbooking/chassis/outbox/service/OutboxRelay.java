package com.hotelbooking.chassis.outbox.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelbooking.chassis.outbox.entity.OutboxEventEntity;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxRelay {

    private final OutboxEventRepository outboxEventRepository;
    @SuppressWarnings("rawtypes")
    private final KafkaTemplate kafkaTemplate;
    private final ObjectMapper objectMapper;

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

    @Scheduled(fixedDelayString = "${outbox.relay.interval-ms:${outbox.publisher.delay:100}}")
    @Transactional
    @SuppressWarnings("unchecked")
    public void relay() {
        List<OutboxEventEntity> events =
            outboxEventRepository.findTop50ByPublishedFalseAndIsDeletedFalseOrderByCreatedAtAsc();

        for (OutboxEventEntity event : events) {
            try {
                String key = extractKafkaKey(event.getPayload());
                String topic = event.getTopic() != null ? event.getTopic() : "booking-events";

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

                event.setPublished(true);
                event.setPublishedAt(Instant.now());
                outboxEventRepository.save(event);

            } catch (Exception ex) {
                log.error("Failed to relay outbox event id={}", event.getId(), ex);
                int retries = event.getRetryCount() != null ? event.getRetryCount() + 1 : 1;
                event.setRetryCount(retries);
                if (retries >= 5) {
                    event.setIsDeleted(true);
                }
                outboxEventRepository.save(event);
            }
        }
    }

    public OutboxEventEntity saveEvent(String topic, Object payloadObject, String eventType) {
        try {
            String payloadStr = payloadObject instanceof String s
                ? s
                : objectMapper.writeValueAsString(payloadObject);

            String traceparent = extractCurrentTraceparent();

            OutboxEventEntity event = OutboxEventEntity.builder()
                .id(UUID.randomUUID())
                .topic(topic)
                .eventType(eventType)
                .payload(payloadStr)
                .published(false)
                .retryCount(0)
                .createdAt(Instant.now())
                .isDeleted(false)
                .traceparent(traceparent)
                .build();

            return outboxEventRepository.save(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox payload", e);
        }
    }

    public OutboxEventEntity saveEvent(String topic, Object payloadObject) {
        String eventType = payloadObject != null ? payloadObject.getClass().getSimpleName() : "UnknownEvent";
        return saveEvent(topic, payloadObject, eventType);
    }

    public OutboxEventEntity saveEvent(Object payloadObject) {
        return saveEvent("booking-events", payloadObject);
    }

    /**
     * Alias for saveEvent to maintain backward compatibility with services calling saveOutboxMessage.
     */
    public OutboxEventEntity saveOutboxMessage(String topic, Object payloadObject, String eventType) {
        return saveEvent(topic, payloadObject, eventType);
    }

    private String extractKafkaKey(String payload) {
        if (payload == null || payload.isBlank()) return null;
        try {
            JsonNode node = objectMapper.readTree(payload);
            String bookingId = node.path("bookingId").asText(null);
            if (bookingId == null || bookingId.isBlank()) {
                bookingId = node.path("booking").path("bookingId").asText(null);
            }
            if (bookingId == null || bookingId.isBlank()) {
                bookingId = node.path("sagaId").asText(null);
            }
            return bookingId;
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
