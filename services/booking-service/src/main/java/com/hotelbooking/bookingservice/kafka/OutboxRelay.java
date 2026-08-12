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
import com.hotelbooking.bookingservice.enums.OutboxEventStatus;
import com.hotelbooking.bookingservice.repository.OutboxEventRepository;
import com.hotelbooking.bookingservice.service.BookingService;

import java.time.Instant;
import java.util.List;
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
    
    private final BookingService bookingService;

    // dùng làm fallback cho Event Listener khi outbox event saved DB
    @Scheduled(fixedDelayString = "${outbox.relay.interval-ms:2000}")
    public void relay() {
        List<OutboxEventEntity> events = bookingService.getAndProcessOutboxEvents();
        bookingService.sendOutboxEventToKafka(events);
    
    }
}
