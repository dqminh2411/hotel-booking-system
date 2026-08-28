package com.hotelbooking.hotelservice;

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
    "com.hotelbooking.hotelservice.entity",
    "com.hotelbooking.chassis.outbox.entity"
})
@EnableJpaRepositories(basePackages = {
    "com.hotelbooking.hotelservice.repository",
    "com.hotelbooking.chassis.outbox.repository"
})
public class HotelServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(HotelServiceApplication.class, args);
	}

}
