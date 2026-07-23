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
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OutboxPublisherService {

    OutboxEventRepository outboxEventRepository;
    KafkaProducerService kafkaProducerService;
    ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${outbox.publisher.delay:100}")
    @Transactional
    public void publishPendingMessages() {
        List<OutboxEventEntity> pending = outboxEventRepository.findTop50ByPublishedFalseAndIsDeletedFalseOrderByCreatedAtAsc();
        for (OutboxEventEntity event : pending) {
            try {
                Object payloadObj = objectMapper.readValue(event.getPayload(), Object.class);
                String eventKey = event.getId().toString();
                kafkaProducerService.send(event.getTopic(), eventKey, payloadObj);

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
            OutboxEventEntity outboxEvent = OutboxEventEntity.builder()
                    .id(UUID.randomUUID())
                    .topic(topic)
                    .payload(payload)
                    .published(false)
                    .isDeleted(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            outboxEventRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox payload", e);
        }
    }
}
