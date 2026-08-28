package com.payment_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EntityScan(basePackages = {
    "com.payment_service.entity",
    "com.hotelbooking.chassis.outbox.entity"
})
@EnableJpaRepositories(basePackages = {
    "com.payment_service.repository",
    "com.hotelbooking.chassis.outbox.repository"
})
public class PaymentServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentServiceApplication.class, args);
	}

}
