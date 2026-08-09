package com.hotelbooking.bookingservice.kafka;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelbooking.bookingservice.entity.OutboxEventEntity;
import com.hotelbooking.bookingservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {
    private static final String FALLBACK_TOPIC = "booking-events";

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
     // TextMapGetter để extract traceparent từ Map khi restore context
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

    @Scheduled(fixedDelayString = "${outbox.relay.interval-ms:100}")
    @Transactional
    public void relay() {
        List<OutboxEventEntity> events =
            outboxEventRepository.findTop100ByPublishedFalseOrderByCreatedAtAsc();
        for (OutboxEventEntity event : events) {
            try {
                JsonNode payloadNode = objectMapper.readTree(event.getPayload());
                String bookingId = payloadNode.path("bookingId").asText(null);
                if (bookingId == null || bookingId.isBlank()) {
                    bookingId = payloadNode.path("booking").path("bookingId").asText(null);
                }
                String topic = event.getTopic() == null || event.getTopic().isBlank()
                    ? FALLBACK_TOPIC
                    : event.getTopic();

                final String key = bookingId;
                // ── Restore OTel trace context + Baggage từ traceparent đã lưu ────────────────
                // Span PRODUCER của kafkaTemplate.send() sẽ đính kèm cả traceparent và baggage vào Kafka headers
                if (event.getTraceparent() != null) {
                    Map<String, String> carrier = new HashMap<>();
                    String tpStr = event.getTraceparent();
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
                    try (io.opentelemetry.context.Scope scope = restoredCtx.makeCurrent()) {
                        kafkaTemplate.send(topic, key, event.getPayload()).get();
                    }
                } else {
                    // Fallback cho row cũ chưa có traceparent
                    kafkaTemplate.send(topic, key, event.getPayload()).get();
                }
                
                event.setPublished(Boolean.TRUE);
                event.setPublishedAt(Instant.now());
                outboxEventRepository.save(event);

            } catch (Exception ex) {
                log.error("Failed to relay outbox event id={}", event.getId(), ex);
            }
        }
    }
}
