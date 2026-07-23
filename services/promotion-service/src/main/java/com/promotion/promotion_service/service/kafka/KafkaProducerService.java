package com.promotion.promotion_service.service.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void send(String topic, String key ,Object message) {
        kafkaTemplate.send(topic, key, message);
        System.out.println("Sent to " + topic + ": " + message);
    }
}
