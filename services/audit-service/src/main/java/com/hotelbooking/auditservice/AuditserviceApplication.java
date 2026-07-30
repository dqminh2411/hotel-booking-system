package com.hotelbooking.auditservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(excludeName = {
        "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration",
        "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration"
})

public class AuditserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditserviceApplication.class, args);
    }

}
