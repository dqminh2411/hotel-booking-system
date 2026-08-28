package com.place_booking_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;

@EnableAsync
@Configuration
public class AsyncConfig implements AsyncConfigurer {
    
    // Cấu hình executor cho task publish outbox event
    @Bean("outboxEventPublish")
    public Executor outboxEventPublishExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // số lượng worker tối thiểu để keep alive mà không bị timeout (kể cả khi worker đó là idle)
        executor.setCorePoolSize(1);
        // số lượng threads tối đa có thể tạo
        executor.setMaxPoolSize(10);
        // kích cỡ queue của worker (sẽ tạo thread mới nếu số lượng items vượt quá capacity của queue)
        executor.setQueueCapacity(100); 
        // tiền tố tên thread
        executor.setThreadNamePrefix("outbox-event-publish-");
        executor.initialize();
        return executor;
    }
}
