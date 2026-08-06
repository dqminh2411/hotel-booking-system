package com.promotion.promotion_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.promotion.promotion_service.entity.OutboxEventEntity;
import com.promotion.promotion_service.repository.OutboxEventRepository;
import com.promotion.promotion_service.service.kafka.KafkaProducerService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OutboxPublisherService {

    OutboxEventRepository outboxEventRepository;
    KafkaProducerService kafkaProducerService;
    ObjectMapper objectMapper;

    // --- TextMapSetter để inject traceparent vào Map (dùng khi lưu outbox) ---
    private static final TextMapSetter<Map<String, String>> MAP_SETTER =
        (carrier, key, value) -> carrier.put(key, value);
    // --- TextMapGetter để extract traceparent từ Map (dùng khi publish) ---
    private static final TextMapGetter<Map<String, String>> MAP_GETTER =
        new TextMapGetter<>() {
            @Override
            public Iterable<String> keys(Map<String, String> carrier) {
                return carrier.keySet();
            }
            @Override
            public String get(Map<String, String> carrier, String key) {
                return carrier.get(key);
            }
        };

    @Scheduled(fixedDelayString = "${outbox.publisher.delay:100}")
    @Transactional
    public void publishPendingMessages() {
        List<OutboxEventEntity> pending = outboxEventRepository.findTop50ByPublishedFalseAndIsDeletedFalseOrderByCreatedAtAsc();
        for (OutboxEventEntity event : pending) {
            try {
                Object payloadObj = objectMapper.readValue(event.getPayload(), Object.class);
                String eventKey = event.getId().toString();
                
                if (event.getTraceparent() != null) {
                    Map<String, String> carrier = new HashMap<>();
                    carrier.put("traceparent", event.getTraceparent());
                    Context restoredCtx = GlobalOpenTelemetry.getPropagators()
                        .getTextMapPropagator()
                        .extract(Context.current(), carrier, MAP_GETTER);
                    try (io.opentelemetry.context.Scope scope = restoredCtx.makeCurrent()) {
                        kafkaProducerService.send(event.getTopic(), eventKey, payloadObj);
                    }
                } else {
                    kafkaProducerService.send(event.getTopic(), eventKey, payloadObj);
                }

                event.setPublished(true);
                event.setPublishedAt(LocalDateTime.now());
            } catch (Exception e) {
                log.error("Publish failed id={}", event.getId(), e);
            }
        }

        outboxEventRepository.saveAll(pending);
    }

    public void saveOutboxMessage(String topic, Object message, String eventType) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            String traceparent = extractCurrentTraceparent();
            OutboxEventEntity outboxEvent = OutboxEventEntity.builder()
                    .topic(topic)
                    .payload(payload)
                    .published(false)
                    .isDeleted(false)
                    .createdAt(LocalDateTime.now())
                    .traceparent(traceparent)
                    .build();
            outboxEventRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox payload", e);
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
