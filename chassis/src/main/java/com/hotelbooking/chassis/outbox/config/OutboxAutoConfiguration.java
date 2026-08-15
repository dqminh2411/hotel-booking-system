package com.hotelbooking.chassis.outbox.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelbooking.chassis.outbox.listener.OutboxEventListener;
import com.hotelbooking.chassis.outbox.repository.OutboxEventRepository;
import com.hotelbooking.chassis.outbox.service.OutboxPublisherService;
import com.hotelbooking.chassis.outbox.service.OutboxRelay;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

@AutoConfiguration
@ConditionalOnClass(KafkaTemplate.class)
@ConditionalOnProperty(name = "chassis.outbox.enabled", havingValue = "true", matchIfMissing = true)
@EnableScheduling
// @EntityScan(basePackages = "com.hotelbooking.chassis.outbox.entity")
// @EnableJpaRepositories(basePackages = "com.hotelbooking.chassis.outbox.repository")
@Import(OutboxAsyncConfig.class)
public class OutboxAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @SuppressWarnings("rawtypes")
    public OutboxPublisherService outboxPublisherService(
            OutboxEventRepository repository,
            KafkaTemplate kafkaTemplate,
            ObjectMapper mapper,
            ApplicationEventPublisher eventPublisher) {
        return new OutboxPublisherService(repository, kafkaTemplate, mapper, eventPublisher);
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxEventListener outboxEventListener(OutboxPublisherService outboxPublisherService) {
        return new OutboxEventListener(outboxPublisherService);
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxRelay outboxEventRelay(OutboxPublisherService outboxPublisherService) {
        return new OutboxRelay(outboxPublisherService);
    }
}
