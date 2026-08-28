package com.place_booking_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients
@EnableScheduling
@EntityScan(basePackages = {
    "com.place_booking_service.entity",
    "com.hotelbooking.chassis.outbox.entity"
})
@EnableJpaRepositories(basePackages = {
    "com.place_booking_service.repository",
    "com.hotelbooking.chassis.outbox.repository"
})
public class PlaceBookingServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PlaceBookingServiceApplication.class, args);
	}

}
