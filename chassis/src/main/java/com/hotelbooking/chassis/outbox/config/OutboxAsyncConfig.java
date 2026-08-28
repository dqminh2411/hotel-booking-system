package com.hotelbooking.chassis.outbox.config;

import java.util.concurrent.Executor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class OutboxAsyncConfig {

    @Bean("outboxEventPublishExecutor")
    @ConditionalOnMissingBean(name = "outboxEventPublishExecutor")
    public Executor outboxEventPublishExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("outbox-event-publish-");
        executor.initialize();
        return executor;
    }
}
