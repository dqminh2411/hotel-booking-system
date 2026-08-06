package com.hotelbooking.hotelservice.kafka;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelbooking.hotelservice.entity.OutboxEventEntity;
import com.hotelbooking.hotelservice.repository.OutboxEventRepository;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OutboxRelay {

    private static final String TOPIC = "hotel-status-actions";

    OutboxEventRepository outboxEventRepository;
    ObjectMapper objectMapper;
    KafkaTemplate<String, String> kafkaTemplate;

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
        List<OutboxEventEntity> events = outboxEventRepository.findTop100ByPublishedFalseOrderByCreatedAtAsc();
        for (OutboxEventEntity event : events) {
            try {
                JsonNode payloadNode = objectMapper.readTree(event.getPayload());
                String hotelId = payloadNode.path("hotelId").asText(null);
                if (hotelId == null || hotelId.isBlank()) {
                    hotelId = payloadNode.path("hotel").path("hotelId").asText(null);
                }

                String topic = event.getTopic() == null || event.getTopic().isBlank()
                    ? TOPIC
                    : event.getTopic();

                if (event.getTraceparent() != null) {
                    Map<String, String> carrier = new HashMap<>();
                    carrier.put("traceparent", event.getTraceparent());
                    Context restoredCtx = GlobalOpenTelemetry.getPropagators()
                        .getTextMapPropagator()
                        .extract(Context.current(), carrier, MAP_GETTER);
                    try (io.opentelemetry.context.Scope scope = restoredCtx.makeCurrent()) {
                        kafkaTemplate.send(topic, hotelId, event.getPayload()).get();
                    }
                } else {
                    kafkaTemplate.send(topic, hotelId, event.getPayload()).get();
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
