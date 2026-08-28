package com.promotion.promotion_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@EntityScan(basePackages = {
    "com.promotion.promotion_service.entity",
    "com.hotelbooking.chassis.outbox.entity"
})
@EnableJpaRepositories(basePackages = {
    "com.promotion.promotion_service.repository",
    "com.hotelbooking.chassis.outbox.repository"
})
public class PromotionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PromotionServiceApplication.class, args);
    }

}
