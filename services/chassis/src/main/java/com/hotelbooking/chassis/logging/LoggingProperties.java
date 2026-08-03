package com.hotelbooking.chassis.logging;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "chassis.logging")
public class LoggingProperties {

    /** Bật/tắt toàn bộ chassis logging */
    private boolean enabled = true;

    /** Bật JSON format (false = plain text cho local dev) */
    private boolean jsonEnabled = true;

    /** Service name, mặc định lấy từ spring.application.name */
    private String serviceName;

    /** Các path không cần log (health check, actuator) */
    private List<String> excludePaths = new ArrayList<>(List.of(
        "/actuator/health",
        "/actuator/info",
        "/actuator/prometheus"
    ));

    /** Bật log business event qua @Loggable annotation */
    private boolean aspectEnabled = true;

    /** Log slow request nếu vượt quá ngưỡng này (ms) */
    private long slowRequestThresholdMs = 1000;

    /** Bật tạo OTel span trong @Loggable aspect */
    private boolean tracingEnabled = true;

    /** Các field nhạy cảm cần mask trong log */
    private List<String> sensitiveFields = new ArrayList<>(List.of(
        "password", "token", "secret", "authorization",
        "vnp_SecureHash", "cardNumber", "cvv"
    ));
}
