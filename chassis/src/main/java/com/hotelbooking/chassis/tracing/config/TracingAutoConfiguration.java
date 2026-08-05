package com.hotelbooking.chassis.tracing.config;


import org.springframework.boot.autoconfigure.AutoConfiguration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;


import com.hotelbooking.chassis.tracing.properties.TracingProperties;


@AutoConfiguration
@EnableConfigurationProperties(TracingProperties.class)
public class TracingAutoConfiguration {
}
