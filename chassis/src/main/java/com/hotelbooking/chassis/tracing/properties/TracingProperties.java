package com.hotelbooking.chassis.tracing.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "management.tracing")
public class TracingProperties {
    
    private Double samplingProbability;

    private boolean enabled = true;
    
}
