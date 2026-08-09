package com.place_booking_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.place_booking_service.entity.OutboxMessage;
import com.place_booking_service.repository.OutboxMessageRepository;
import com.place_booking_service.service.kafka.KafkaProducerService;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.api.GlobalOpenTelemetry;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OutboxPublisherService {

    OutboxMessageRepository outboxMessageRepository;
    KafkaProducerService kafkaProducerService;
    ObjectMapper objectMapper;

     // TextMapSetter để inject traceparent vào Map (dùng khi lưu outbox)
    private static final TextMapSetter<Map<String, String>> MAP_SETTER =
        (carrier, key, value) -> carrier.put(key, value);
    // TextMapGetter để extract traceparent từ Map (dùng khi publish)
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
                String bookingId= payload.path("bookingId").asText();
               
                Object payloadObj = objectMapper.readValue(message.getPayload(), Object.class);
                
                // ── Restore OTel trace context từ traceparent đã lưu ──────────────
                // Điều này gắn span producer vào đúng trace của HTTP request gốc
                if (message.getTraceparent() != null) {
                    Map<String, String> carrier = new HashMap<>();
                    String tpStr = message.getTraceparent();
                    if (tpStr.startsWith("{")) {
                        try {
                            Map<String, String> map = objectMapper.readValue(tpStr, Map.class);
                            carrier.putAll(map);
                        } catch (Exception e) {
                            carrier.put("traceparent", tpStr);
                        }
                    } else {
                        carrier.put("traceparent", tpStr);
                    }
                    Context restoredCtx = GlobalOpenTelemetry.getPropagators()
                        .getTextMapPropagator()
                        .extract(Context.current(), carrier, MAP_GETTER);
                    // Chạy send trong context đã restore → Java Agent tự tạo
                    // child span PRODUCER và đính kèm W3C headers (traceparent + baggage) vào Kafka record
                    try (io.opentelemetry.context.Scope scope = restoredCtx.makeCurrent()) {
                        kafkaProducerService.send(message.getTopic(), bookingId, payloadObj);
                    }
                } else {
                    // Fallback nếu không có traceparent (row cũ)
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

     /**
     * Lưu outbox message kèm W3C traceparent và Baggage của request hiện tại.
     * Context được capture ngay trong HTTP request thread → có valid SpanContext + Baggage.
     */
    public void saveOutboxMessage(String topic, Object message, String eventType) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            // ── Capture traceparent + baggage từ context hiện tại ──────────
            String traceparent = extractCurrentContextCarrier();
            OutboxMessage outboxMessage = new OutboxMessage();
            outboxMessage.setId(UUID.randomUUID());
            outboxMessage.setEventType(eventType);
            outboxMessage.setTopic(topic);
            outboxMessage.setPayload(payload);
            outboxMessage.setStatus("PENDING");
            outboxMessage.setRetryCount(0);
            outboxMessage.setCreatedAt(LocalDateTime.now());
            outboxMessage.setTraceparent(traceparent);
            outboxMessageRepository.save(outboxMessage);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox payload", e);
        }
    }

    /**
     * Lấy W3C headers (traceparent + baggage) từ current OTel Context.
     */
    private String extractCurrentContextCarrier() {
        Map<String, String> carrier = new HashMap<>();
        GlobalOpenTelemetry.getPropagators()
            .getTextMapPropagator()
            .inject(Context.current(), carrier, MAP_SETTER);
        if (carrier.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(carrier);
        } catch (Exception e) {
            return carrier.get("traceparent");
        }
    }
}
