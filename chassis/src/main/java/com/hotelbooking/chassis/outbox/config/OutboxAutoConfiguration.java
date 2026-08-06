package com.hotelbooking.chassis.outbox.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelbooking.chassis.outbox.repository.OutboxEventRepository;
import com.hotelbooking.chassis.outbox.service.OutboxRelay;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

@AutoConfiguration
@ConditionalOnClass(KafkaTemplate.class)
@ConditionalOnProperty(name = "chassis.outbox.enabled", havingValue = "true", matchIfMissing = true)
@EnableScheduling
public class OutboxAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @SuppressWarnings("rawtypes")
    public OutboxRelay outboxEventRelay(OutboxEventRepository repository, KafkaTemplate kafkaTemplate, ObjectMapper mapper) {
        return new OutboxRelay(repository, kafkaTemplate, mapper);
    }
}
