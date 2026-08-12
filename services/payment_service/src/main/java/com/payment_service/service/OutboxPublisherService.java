package com.payment_service.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment_service.dto.OutboxEventPublishEvent;
import com.payment_service.entity.OutboxEventEntity;
import com.payment_service.enums.OutboxEventStatus;
import com.payment_service.repository.OutboxEventRepository;
import com.payment_service.service.kafka.KafkaProducerService;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OutboxPublisherService {

    private static final int MAX_RETRIES = 5;

    OutboxEventRepository outboxEventRepository;
    KafkaProducerService kafkaProducerService;
    ObjectMapper objectMapper;
    ApplicationEventPublisher eventPublisher;
    @Autowired
    private OutboxMessageRepository outboxMessageRepository;
    @Autowired
    private KafkaProducerService kafkaProducerService;
    @Autowired
    private ObjectMapper objectMapper;

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
        List<OutboxMessage> pending = outboxMessageRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING");
        for (OutboxMessage message : pending) {
            try {
                JsonNode payload = objectMapper.readTree(message.getPayload());
                String bookingId = payload.path("bookingId").asText();
                Object payloadObj = objectMapper.readValue(message.getPayload(), Object.class);
                
                if (message.getTraceparent() != null) {
                    Map<String, String> carrier = new HashMap<>();
                    carrier.put("traceparent", message.getTraceparent());
                    Context restoredCtx = GlobalOpenTelemetry.getPropagators()
                        .getTextMapPropagator()
                        .extract(Context.current(), carrier, MAP_GETTER);
                    try (io.opentelemetry.context.Scope scope = restoredCtx.makeCurrent()) {
                        kafkaProducerService.send(message.getTopic(), bookingId, payloadObj);
                    }
                } else {
                    kafkaProducerService.send(message.getTopic(), bookingId, payloadObj);
                }

                message.setStatus("PROCESSED");
                message.setPublishedAt(LocalDateTime.now());

            } catch (Exception e) {
                log.error("Publish failed id={}", message.getId(), e);

                message.setRetryCount(message.getRetryCount() + 1);

                if (message.getRetryCount() >= 5) {
                    message.setStatus("FAILED");
                }
            }
        }

        outboxMessageRepository.saveAll(pending);
    }

    public void saveOutboxMessage(String topic, Object message, String eventType) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            String traceparent = extractCurrentTraceparent();
            OutboxEventEntity outbox = new OutboxEventEntity();
            outbox.setId(UUID.randomUUID());
            outbox.setTopic(topic);
            outbox.setPayload(payload);
            outbox.setStatus(OutboxEventStatus.PENDING.toString());
            outbox.setCreatedAt(Instant.now());
            outbox.setRetryCount(0);
            outboxMessage.setTraceparent(traceparent);
            outboxEventRepository.save(outbox);

            eventPublisher.publishEvent(new OutboxEventPublishEvent());
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

    /***
     * lấy Outbox event, 
     * nếu status hiện tại là PENDING thì đánh dấu status = PROCESSING
     * nếu status hiện tại là PROCESSING thì 
     * - nếu retry_count >= MAX_RETRIES thì đánh dấu status = DEAD_LETTER
     * - nếu retry_count < MAX_RETRIES thì tính thời gian retry lần kế tiếp nếu lần gửi này thất bại sử dụng exponential backoff và jitter.
     * set thời gian lock timeout: locked_until = current_time + TIMEOUT_IN_SECONDS
     */
    @Transactional
    public List<OutboxEventEntity> getAndProcessOutboxEvents() {
        int TIMEOUT_IN_SECONDS = 30;
        List<OutboxEventEntity> events = outboxEventRepository.findEventsToPublish();
        events.forEach(event -> {
            if (OutboxEventStatus.PENDING.toString().equals(event.getStatus())) {
                event.setStatus(OutboxEventStatus.PROCESSING.toString());
            } else {
                event.setRetryCount(event.getRetryCount() + 1);
                if (event.getRetryCount() >= MAX_RETRIES) {
                    event.setStatus(OutboxEventStatus.DEAD_LETTER.toString());
                } else {
                    long baseDelay = (long) Math.pow(2, event.getRetryCount());
                    long jitter = ThreadLocalRandom.current().nextLong(baseDelay / 2 + 1); // 0 - 50% of baseDelay
                    event.setNextRetryAt(Instant.now().plusSeconds(baseDelay + jitter));
                }
            }
            event.setLockedUntil(Instant.now().plusSeconds(TIMEOUT_IN_SECONDS));
        });

        return outboxEventRepository.saveAll(events);
    }

    // gửi outbox event tới Kafka
    public void sendOutboxEventToKafka(List<OutboxEventEntity> events) {
        for (OutboxEventEntity event : events) {
            try {
                JsonNode payloadNode = objectMapper.readTree(event.getPayload());
                String bookingId = payloadNode.path("bookingId").asText(null);
                if (bookingId == null || bookingId.isBlank()) {
                    bookingId = payloadNode.path("booking").path("bookingId").asText(null);
                }
                String topic = event.getTopic();
                Object payloadObj = objectMapper.readValue(event.getPayload(), Object.class);

                kafkaProducerService.send(topic, bookingId, payloadObj);

                event.setStatus(OutboxEventStatus.PUBLISHED.toString());
                event.setPublishedAt(Instant.now());
                outboxEventRepository.save(event);
            } catch (Exception ex) {
                log.error("Failed to relay outbox event id={}", event.getId(), ex);
            }
        }
    }
}
