package com.hotelbooking.bookingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableFeignClients
@EntityScan(basePackages = {
    "com.hotelbooking.bookingservice.entity",
    "com.hotelbooking.chassis.outbox.entity"
})
@EnableJpaRepositories(basePackages = {
    "com.hotelbooking.bookingservice.repository",
    "com.hotelbooking.chassis.outbox.repository"
})
public class BookingServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(BookingServiceApplication.class, args);
	}

}
