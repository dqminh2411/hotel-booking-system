package com.hotelbooking.hotelservice.kafka;

import java.time.Instant;
import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelbooking.hotelservice.entity.OutboxEventEntity;
import com.hotelbooking.hotelservice.repository.OutboxEventRepository;

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
                kafkaTemplate.send(topic, hotelId, event.getPayload()).get();

                event.setPublished(Boolean.TRUE);
                event.setPublishedAt(Instant.now());
                outboxEventRepository.save(event);
            } catch (Exception ex) {
                log.error("Failed to relay outbox event id={}", event.getId(), ex);
            }
        }
    }
}
