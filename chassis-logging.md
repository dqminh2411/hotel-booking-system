# Tài liệu thiết kế và cài đặt: Centralized Logging Chassis Module

**Dự án:** HotelHub SaaS Platform  
**Phiên bản:** 1.1  
**Ngày:** 03/08/2026  
**Phạm vi:** Chassis module dùng chung cho tất cả microservices

---

## Mục lục

- [1. Phân tích yêu cầu và tư duy hệ thống](#1-phân-tích-yêu-cầu-và-tư-duy-hệ-thống)
- [2. Những vấn đề cần cân nhắc](#2-những-vấn-đề-cần-cân-nhắc)
- [3. Lựa chọn công nghệ](#3-lựa-chọn-công-nghệ)
- [4. Kiến trúc tổng thể](#4-kiến-trúc-tổng-thể)
- [5. Thiết kế Chassis Module](#5-thiết-kế-chassis-module)
- [6. Cài đặt chi tiết](#6-cài-đặt-chi-tiết)
- [7. Hướng dẫn sử dụng tại các service](#7-hướng-dẫn-sử-dụng-tại-các-service)
- [8. Cấu hình hạ tầng ELK Stack](#8-cấu-hình-hạ-tầng-elk-stack)
- [9. Best practices](#9-best-practices)
- [10. Khả năng thay đổi log stack](#10-khả-năng-thay-đổi-log-stack)
- [11. Tích hợp OpenTelemetry + Jaeger (Distributed Tracing)](#11-tích-hợp-opentelemetry--jaeger-distributed-tracing)

---

## 1. Phân tích yêu cầu và tư duy hệ thống

### 1.1. Câu hỏi nền tảng: Log để làm gì?

Trước khi chọn công nghệ, cần xác định mục đích của log. Có ba mục đích thực sự khác nhau, mỗi loại có yêu cầu riêng về format, retention và access control:

| Mục đích | Câu hỏi trả lời | Ví dụ trong HotelHub |
| --- | --- | --- |
| **Debugging** | Tại sao request X bị lỗi? | Exception trong Saga đặt phòng, thanh toán thất bại |
| **Observability** | Hệ thống đang hoạt động như thế nào? | Request rate, error rate, latency theo service |
| **Business Audit** | Ai đã làm gì, lúc nào? | Ai đặt phòng, ai duyệt khách sạn, ai xóa ảnh vi phạm |

Tài liệu này tập trung vào **Debugging** và **Observability** thông qua centralized logging. Business Audit Log được xử lý riêng qua MongoDB Audit Service (tài liệu khác).

### 1.2. Tư duy thiết kế

**Service không nên biết log đi đâu.** Service chỉ cần viết log có cấu trúc ra stdout. Việc log được thu thập, vận chuyển, lưu trữ ở đâu là trách nhiệm của hạ tầng. Đây là nguyên tắc quan trọng nhất, quyết định khả năng thay đổi log stack sau này mà không cần sửa code service.

```
Service viết log → [stdout] → [Hạ tầng quyết định đi đâu]
                                    ↓
                              ELK Stack (hiện tại)
                              OTel + Loki + Grafana (tương lai)
                              Bất kỳ stack nào khác
```

---

## 2. Những vấn đề cần cân nhắc

### 2.1. Correlation — Vấn đề quan trọng nhất

Trong kiến trúc microservices, một request từ client đi qua nhiều service. Không có cơ chế correlation, các dòng log là những mảnh rời rạc vô nghĩa.

```
Request từ Client
    → API Gateway          (tạo traceId: abc123)
        → Place Booking    (propagate traceId: abc123)
            → Booking      (propagate traceId: abc123)
            → Payment      (propagate traceId: abc123)
                → Kafka event  (kèm traceId: abc123)
                    → Notification (propagate traceId: abc123)
```

Khi tìm kiếm `traceId = abc123`, toàn bộ hành trình của một request hiện ra từ tất cả service.

**Quyết định:** API Gateway tạo `traceId`, các service downstream chỉ propagate qua HTTP header `X-Trace-Id`. Kafka message kèm `traceId` trong header để consumer tiếp tục chuỗi correlation.

### 2.2. Structured Log — JSON là bắt buộc

Log plain text không thể query hiệu quả. Mọi log phải là JSON để Elasticsearch có thể index và query theo từng field.

```
# Plain text — không thể query theo userId
2026-07-27 10:30:15 ERROR BookingService - Failed to reserve room for user 123

# Structured JSON — query được theo mọi field
{
  "@timestamp": "2026-07-27T10:30:15.123Z",
  "severity":   "ERROR",
  "service":    "booking-service",
  "traceId":    "abc123",
  "userId":     "user-123",
  "hotelId":    "hotel-456",
  "clientIp":   "113.185.54.22",
  "event":      "ROOM_RESERVATION_FAILED",
  "message":    "Failed to reserve room",
  "duration_ms": 45
}
```

### 2.3. Log Level — Cân bằng giữa chi phí và khả năng debug

| Level | Dùng khi nào | Production |
| --- | --- | --- |
| ERROR | Lỗi nghiêm trọng, cần xử lý ngay | Luôn bật |
| WARN | Vấn đề tiềm ẩn, không crash | Luôn bật |
| INFO | Sự kiện nghiệp vụ quan trọng | Bật |
| DEBUG | Chi tiết kỹ thuật khi debug | Tắt ở production |
| TRACE | Rất chi tiết | Không dùng ở production |

Log DEBUG ở production có thể làm tăng volume log 10–100 lần, gây tốn chi phí lưu trữ và làm chậm hệ thống.

### 2.4. Dữ liệu nhạy cảm — Không được log

- Password, token, API key
- Số thẻ tín dụng, VNPay secret hash
- CCCD, passport, thông tin định danh cá nhân
- Toàn bộ request/response body (có thể chứa dữ liệu nhạy cảm)

Chassis module phải có cơ chế masking tự động, không phụ thuộc vào developer nhớ.

### 2.5. Vận chuyển Log — Pull tốt hơn Push

```
Push: Service → [HTTP/gRPC] → Logstash/Collector (tạo coupling)
Pull: Service → [stdout] → Agent đọc → Logstash (service không quan tâm hạ tầng)
```

Dùng **Pull**: service ghi ra stdout, Filebeat/Logstash đọc từ Docker container logs. Service không bị ảnh hưởng nếu Logstash tạm thời chết.

### 2.6. Context đa tầng

Mỗi dòng log cần mang đủ context để có thể đứng độc lập mà vẫn hiểu được:

```
Technical context:  traceId, spanId, service, hostname, thread, timestamp
Business context:   userId, tenantId, hotelId, bookingId (tùy domain)
Request context:    HTTP method, path, status, duration, clientIp
```

Java MDC (Mapped Diagnostic Context) cho phép gắn context vào mọi dòng log của một thread mà không cần truyền tay qua từng method.

---

## 3. Lựa chọn công nghệ

### 3.1. Stack hiện tại: ELK

```
Ghi log:        Logback + logstash-logback-encoder
Context:        MDC (Java built-in)
Format:         JSON structured
Vận chuyển:     stdout → Filebeat → Logstash
Lưu trữ:        Elasticsearch
Trực quan:      Kibana
```

### 3.2. Lý do lựa chọn

**Logback + logstash-logback-encoder:**
- Logback là default của Spring Boot, không cần thay dependency
- `logstash-logback-encoder` là thư viện chuẩn de facto để output JSON từ Logback trong hệ sinh thái Java
- Cộng đồng lớn, tài liệu phong phú, ổn định trong production

**ELK Stack (Elasticsearch + Logstash + Kibana):**
- Elasticsearch có khả năng full-text search và aggregation mạnh
- Kibana cung cấp visualize và query trực quan
- Logstash có pipeline transform linh hoạt

**Tại sao thiết kế để dễ thay stack:** Vì ELK tốn tài nguyên RAM đáng kể ở production. Tương lai có thể chuyển sang OTel + Loki + Grafana (nhẹ hơn, chi phí thấp hơn) hoặc stack khác mà không sửa code service.

### 3.3. Bảng so sánh stack

| Tiêu chí | ELK | OTel + Loki + Grafana |
| --- | --- | --- |
| RAM consumption | Cao (ES cần 2–4GB min) | Thấp (Loki lưu theo stream) |
| Full-text search | Rất mạnh | Tốt với LogQL |
| Tích hợp traces | Cần Jaeger/Zipkin riêng | Grafana tích hợp Tempo/Jaeger |
| Chi phí vận hành | Cao hơn | Thấp hơn |
| Độ chín | Rất cao | Đang tăng nhanh |
| Phù hợp hiện tại | ✅ | Tương lai |

---

## 4. Kiến trúc tổng thể

### 4.1. Luồng dữ liệu

```
[HTTP Request]
      ↓
[MdcFilter] ─── gắn traceId, tenantId, userId, clientIp vào MDC
      ↓
[@Loggable Aspect] ─── bắt đầu ghi business event log
      ↓
[Business Logic] ─── log.info() / log.error() bình thường
      ↓
[@Loggable Aspect] ─── ghi kết quả + duration
      ↓
[Logback Encoder] ─── serialize MDC + message → JSON
      ↓
[stdout container]
      ↓
[Filebeat] ─── đọc container log, parse JSON
      ↓
[Logstash] ─── enrich metadata, route theo service
      ↓
[Elasticsearch] ─── index: hotelhub-logs-{service}-{yyyy.MM.dd}
      ↓
[Kibana] ─── query, dashboard, alert

[Kafka Consumer]
      ↓
[KafkaLoggingUtils.setupMdc(record)] ─── đọc traceId từ Kafka header
      ↓
[Business Logic] ─── log với context đầy đủ
      ↓
[KafkaLoggingUtils.clearMdc()]
```

### 4.2. Cấu trúc Chassis Module

```
hotelhub-chassis/
├── pom.xml
└── src/main/
    ├── java/com/hotelhub/chassis/
    │   └── logging/
    │       ├── LoggingAutoConfiguration.java      # Spring Boot auto-config
    │       ├── LoggingProperties.java             # Cấu hình từ yml
    │       ├── filter/
    │       │   └── MdcFilter.java                 # HTTP request filter
    │       ├── kafka/
    │       │   ├── MdcKafkaProducerInterceptor.java
    │       │   └── KafkaLoggingUtils.java
    │       ├── aop/
    │       │   ├── Loggable.java                  # Custom annotation
    │       │   └── LoggingAspect.java             # AOP aspect
    │       └── util/
    │           ├── LoggingUtils.java              # Utility methods
    │           └── SensitiveDataMasker.java       # Masking dữ liệu nhạy cảm
    └── resources/
        ├── META-INF/spring/
        │   └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
        └── logback-spring.xml                     # Template JSON config
```

---

## 5. Thiết kế Chassis Module

### 5.1. Nguyên tắc thiết kế

**Nguyên tắc 1 — Zero config:** Service import dependency là xong, không cần cấu hình thêm gì. Auto-configuration của Spring Boot tự kích hoạt.

**Nguyên tắc 2 — Tách biệt format và transport:** Chassis chỉ chịu trách nhiệm format log thành JSON chuẩn. Việc log đi đến Elasticsearch hay Loki là quyết định của hạ tầng, không phải của code service.

**Nguyên tắc 3 — Override được:** Service có thể override bất kỳ cấu hình nào từ chassis nếu cần thiết cho use case đặc thù.

**Nguyên tắc 4 — Fail safe:** Lỗi trong logging không được làm hỏng business logic. Mọi logic logging phải được bọc trong try-catch riêng.

### 5.2. Schema log chuẩn

Mọi dòng log trong hệ thống phải tuân thủ schema sau để Elasticsearch có thể index nhất quán:

```json
{
  "@timestamp":     "2026-07-27T10:30:15.123Z",
  "severity":       "INFO",
  "service":        "booking-service",
  "traceId":        "abc123def456",
  "spanId":         "span789",
  "tenantId":       "tenant-001",
  "userId":         "user-123",
  "clientIp":       "113.185.54.22",
  "thread":         "http-nio-8080-exec-3",
  "logger":         "com.hotelhub.booking.service.BookingService",
  "event":          "BOOKING_CREATED",
  "message":        "Booking created successfully",
  "bookingId":      "booking-456",
  "hotelId":        "hotel-789",
  "duration_ms":    145,
  "http.method":    "POST",
  "http.path":      "/api/place-booking",
  "http.status":    202
}
```

**Quy tắc đặt tên field:**
- Snake_case cho tất cả field: `booking_id` không phải `bookingId` (ELK convention)
- Ngoại lệ: `traceId`, `spanId`, `tenantId`, `userId` dùng camelCase để tương thích với OTel convention
- Field domain-specific (bookingId, hotelId) thêm tên entity: không dùng chỉ `id`

---

## 6. Cài đặt chi tiết

### 6.1. `pom.xml` của chassis module

```xml
<project>
    <groupId>com.hotelhub</groupId>
    <artifactId>hotelhub-chassis</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <dependencies>
        <!-- Spring Boot (provided — service đã có) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-autoconfigure</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- JSON encoder cho Logback -->
        <dependency>
            <groupId>net.logstash.logback</groupId>
            <artifactId>logstash-logback-encoder</artifactId>
            <version>7.4</version>
        </dependency>

        <!-- Logback (provided — Spring Boot đã kéo vào) -->
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- Jakarta Servlet (cho MdcFilter) -->
        <dependency>
            <groupId>jakarta.servlet</groupId>
            <artifactId>jakarta.servlet-api</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- Kafka (optional — chỉ active nếu service dùng Kafka) -->
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
            <scope>provided</scope>
            <optional>true</optional>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
    </dependencies>
</project>
```

### 6.2. `LoggingProperties.java` — Cấu hình từ `application.yml`

```java
package com.hotelhub.chassis.logging;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties(prefix = "chassis.logging")
public class LoggingProperties {

    /** Bật/tắt toàn bộ chassis logging */
    private boolean enabled = true;

    /** Bật JSON format (false = plain text cho local dev) */
    private boolean jsonEnabled = true;

    /** Service name, mặc định lấy từ spring.application.name */
    private String serviceName;

    /** Các path không cần log (health check, actuator) */
    private List<String> excludePaths = List.of(
        "/actuator/health",
        "/actuator/info",
        "/actuator/prometheus"
    );

    /** Bật log business event qua @Loggable annotation */
    private boolean aspectEnabled = true;

    /** Log slow request nếu vượt quá ngưỡng này (ms) */
    private long slowRequestThresholdMs = 1000;

    /** Các field nhạy cảm cần mask trong log */
    private List<String> sensitiveFields = List.of(
        "password", "token", "secret", "authorization",
        "vnp_SecureHash", "cardNumber", "cvv"
    );

    // getters & setters
}
```

### 6.3. `logback-spring.xml` — Cấu hình Logback cho toàn hệ thống

File này đặt trong `src/main/resources` của chassis, sẽ được auto-discover bởi Spring Boot khi service import chassis.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>

    <!-- Lấy config từ application.yml qua Spring Environment -->
    <springProperty name="SERVICE_NAME"
                    source="spring.application.name"
                    defaultValue="unknown-service"/>
    <springProperty name="LOG_LEVEL_ROOT"
                    source="logging.level.root"
                    defaultValue="INFO"/>
    <springProperty name="JSON_ENABLED"
                    source="chassis.logging.json-enabled"
                    defaultValue="true"/>

    <!-- ==================== APPENDER: Plain Text (local dev) ==================== -->
    <appender name="CONSOLE_TEXT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36}
                     [%X{traceId:-NO_TRACE}] [%X{tenantId:-}]
                     - %msg%n%ex</pattern>
        </encoder>
    </appender>

    <!-- ==================== APPENDER: JSON (staging/production) ==================== -->
    <appender name="CONSOLE_JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">

            <!-- Tắt caller data (chậm và ít dùng) -->
            <includeCallerData>false</includeCallerData>

            <!-- Field cố định: service name -->
            <customFields>{"service":"${SERVICE_NAME}"}</customFields>

            <!-- Đổi tên field mặc định để khớp ELK convention -->
            <fieldNames>
                <timestamp>@timestamp</timestamp>
                <message>message</message>
                <logger>logger</logger>
                <thread>thread</thread>
                <levelValue>[ignore]</levelValue>
                <!-- OTel dùng 'severity' thay vì 'level' để dễ migrate sau -->
                <level>severity</level>
            </fieldNames>

            <!-- MDC fields tự động được include.
                 Liệt kê tường minh để kiểm soát thứ tự và tránh field không mong muốn -->
            <includeMdcKeyName>traceId</includeMdcKeyName>
            <includeMdcKeyName>spanId</includeMdcKeyName>
            <includeMdcKeyName>tenantId</includeMdcKeyName>
            <includeMdcKeyName>userId</includeMdcKeyName>
            <includeMdcKeyName>clientIp</includeMdcKeyName>
            <includeMdcKeyName>requestId</includeMdcKeyName>
            <includeMdcKeyName>http.method</includeMdcKeyName>
            <includeMdcKeyName>http.path</includeMdcKeyName>

            <!-- Không include stack trace inline — Logstash sẽ xử lý riêng -->
            <throwableConverter class="net.logstash.logback.stacktrace.ShortenedThrowableConverter">
                <maxDepthPerCause>20</maxDepthPerCause>
                <shortenedClassNameLength>30</shortenedClassNameLength>
                <rootCauseFirst>true</rootCauseFirst>
            </throwableConverter>

        </encoder>
    </appender>

    <!-- Async wrapper — không block business thread khi ghi log -->
    <appender name="ASYNC_JSON" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="CONSOLE_JSON"/>
        <!-- Queue 512 event, không discard khi đầy (block thay vì mất log) -->
        <queueSize>512</queueSize>
        <discardingThreshold>0</discardingThreshold>
        <includeCallerData>false</includeCallerData>
        <neverBlock>false</neverBlock>
    </appender>

    <!-- ==================== PROFILE ROUTING ==================== -->

    <!-- Local dev: plain text dễ đọc -->
    <springProfile name="local,default">
        <root level="${LOG_LEVEL_ROOT}">
            <appender-ref ref="CONSOLE_TEXT"/>
        </root>
    </springProfile>

    <!-- Staging & Production: JSON async -->
    <springProfile name="staging,production">
        <root level="${LOG_LEVEL_ROOT}">
            <appender-ref ref="ASYNC_JSON"/>
        </root>

        <!-- Tắt verbose log của các thư viện third-party -->
        <logger name="org.hibernate.SQL"                    level="WARN"/>
        <logger name="org.hibernate.orm.jdbc.bind"         level="WARN"/>
        <logger name="org.springframework.web"              level="WARN"/>
        <logger name="org.springframework.security"         level="WARN"/>
        <logger name="io.netty"                             level="WARN"/>
        <logger name="org.apache.kafka"                     level="WARN"/>
        <logger name="com.zaxxer.hikari"                    level="INFO"/>
    </springProfile>
</configuration>
```

### 6.4. `MdcFilter.java` — Gắn context vào mọi HTTP request

```java
package com.hotelhub.chassis.logging.filter;

import com.hotelhub.chassis.logging.LoggingProperties;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)  // Chạy trước mọi filter khác
@RequiredArgsConstructor
public class MdcFilter extends OncePerRequestFilter {

    // Header names — phải đồng nhất toàn hệ thống
    public static final String TRACE_ID_HEADER  = "X-Trace-Id";
    public static final String TENANT_ID_HEADER = "X-Tenant-Id";
    public static final String USER_ID_HEADER   = "X-User-Id";

    // MDC key names — phải khớp với field names trong logback-spring.xml
    private static final String MDC_TRACE_ID   = "traceId";
    private static final String MDC_SPAN_ID    = "spanId";
    private static final String MDC_TENANT_ID  = "tenantId";
    private static final String MDC_USER_ID    = "userId";
    private static final String MDC_CLIENT_IP  = "clientIp";
    private static final String MDC_REQUEST_ID = "requestId";
    private static final String MDC_HTTP_METHOD = "http.method";
    private static final String MDC_HTTP_PATH   = "http.path";

    private final LoggingProperties properties;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return properties.getExcludePaths().stream()
            .anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
                                    throws ServletException, IOException {
        long startTime = System.currentTimeMillis();

        try {
            setupMdc(request);
            // Forward traceId xuống service tiếp theo
            response.setHeader(TRACE_ID_HEADER, MDC.get(MDC_TRACE_ID));

            chain.doFilter(request, response);

        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logRequestCompleted(request, response, duration);

            // QUAN TRỌNG: Clear MDC sau mỗi request.
            // Thread pool tái sử dụng thread — nếu không clear,
            // request sau sẽ mang context của request trước.
            MDC.clear();
        }
    }

    private void setupMdc(HttpServletRequest request) {
        // traceId: lấy từ header (Gateway đã tạo) hoặc tạo mới
        String traceId = Optional.ofNullable(request.getHeader(TRACE_ID_HEADER))
            .filter(id -> !id.isBlank())
            .orElseGet(() -> UUID.randomUUID().toString().replace("-", ""));

        MDC.put(MDC_TRACE_ID,   traceId);
        MDC.put(MDC_SPAN_ID,    UUID.randomUUID().toString().substring(0, 16));
        MDC.put(MDC_TENANT_ID,  getHeaderOrDefault(request, TENANT_ID_HEADER, "platform"));
        MDC.put(MDC_USER_ID,    getHeaderOrDefault(request, USER_ID_HEADER, "anonymous"));
        MDC.put(MDC_CLIENT_IP,  extractClientIp(request));
        MDC.put(MDC_REQUEST_ID, UUID.randomUUID().toString());
        MDC.put(MDC_HTTP_METHOD, request.getMethod());
        MDC.put(MDC_HTTP_PATH,   request.getRequestURI());
    }

    private void logRequestCompleted(HttpServletRequest request,
                                     HttpServletResponse response,
                                     long durationMs) {
        int status = response.getStatus();

        // Slow request warning
        if (durationMs > properties.getSlowRequestThresholdMs()) {
            log.warn("Slow request detected",
                StructuredArguments.kv("event", "SLOW_REQUEST"),
                StructuredArguments.kv("duration_ms", durationMs),
                StructuredArguments.kv("threshold_ms", properties.getSlowRequestThresholdMs()));
        }

        if (status >= 500) {
            log.error("HTTP request completed with server error",
                StructuredArguments.kv("event", "HTTP_REQUEST_COMPLETED"),
                StructuredArguments.kv("http.status", status),
                StructuredArguments.kv("duration_ms", durationMs));
        } else if (status >= 400) {
            log.warn("HTTP request completed with client error",
                StructuredArguments.kv("event", "HTTP_REQUEST_COMPLETED"),
                StructuredArguments.kv("http.status", status),
                StructuredArguments.kv("duration_ms", durationMs));
        } else {
            log.info("HTTP request completed",
                StructuredArguments.kv("event", "HTTP_REQUEST_COMPLETED"),
                StructuredArguments.kv("http.status", status),
                StructuredArguments.kv("duration_ms", durationMs));
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        // Xử lý trường hợp đứng sau reverse proxy / load balancer
        return Optional.ofNullable(request.getHeader("X-Forwarded-For"))
            .map(xff -> xff.split(",")[0].trim())
            .orElseGet(() -> Optional.ofNullable(request.getHeader("X-Real-IP"))
                .orElse(request.getRemoteAddr()));
    }

    private String getHeaderOrDefault(HttpServletRequest request,
                                      String headerName, String defaultValue) {
        return Optional.ofNullable(request.getHeader(headerName))
            .filter(v -> !v.isBlank())
            .orElse(defaultValue);
    }
}
```

### 6.5. `Loggable.java` — Custom annotation cho business method

```java
package com.hotelhub.chassis.logging.aop;

import java.lang.annotation.*;

/**
 * Đánh dấu method cần ghi structured business event log tự động.
 *
 * Aspect sẽ tự động log:
 *  - Khi method bắt đầu (với các param đã annotate @LogParam)
 *  - Khi method hoàn tất (với duration)
 *  - Khi method throw exception (với error detail)
 *
 * Mọi log đều kèm clientIp, traceId, userId, tenantId từ MDC.
 *
 * Ví dụ:
 * <pre>
 *   {@literal @}Loggable(event = "BOOKING_CREATED", message = "Create a booking")
 *   public BookingResponse createBooking(BookingRequest request) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Loggable {

    /**
     * Tên sự kiện nghiệp vụ — dùng UPPER_SNAKE_CASE.
     * Ví dụ: "BOOKING_CREATED", "PAYMENT_SUCCESS", "HOTEL_APPROVED"
     */
    String event();

    /**
     * Mô tả hành động bằng ngôn ngữ tự nhiên, xuất hiện trong field "message".
     * Ví dụ: "Create a booking", "Process payment", "Approve hotel listing"
     */
    String message();

    /**
     * Log level khi thành công. Mặc định INFO.
     */
    LogLevel successLevel() default LogLevel.INFO;

    /**
     * Log khi bắt đầu method không? Mặc định true.
     * Tắt nếu method được gọi rất thường xuyên (ví dụ: health check internal).
     */
    boolean logOnEntry() default true;

    /**
     * Đưa giá trị trả về vào log không? Mặc định false.
     * Chỉ bật khi return type không chứa dữ liệu nhạy cảm.
     */
    boolean logReturnValue() default false;

    enum LogLevel { DEBUG, INFO, WARN }
}
```

### 6.6. `LogParam.java` — Đánh dấu parameter cần log

```java
package com.hotelhub.chassis.logging.aop;

import java.lang.annotation.*;

/**
 * Đánh dấu parameter của method được annotate @Loggable sẽ được đưa vào log.
 *
 * Ví dụ:
 * <pre>
 *   {@literal @}Loggable(event = "BOOKING_CREATED", message = "Create a booking")
 *   public BookingResponse createBooking(
 *       {@literal @}LogParam("hotelId") String hotelId,
 *       {@literal @}LogParam("roomTypeId") String roomTypeId,
 *       BookingRequest request  // không có @LogParam → không log
 *   ) { ... }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogParam {

    /** Tên field trong log output */
    String value();

    /** Mask giá trị (thay bằng ***) — dùng cho field nhạy cảm */
    boolean sensitive() default false;
}
```

### 6.7. `LoggingAspect.java` — AOP Aspect xử lý annotation

```java
package com.hotelhub.chassis.logging.aop;

import com.hotelhub.chassis.logging.util.SensitiveDataMasker;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArgument;
import net.logstash.logback.argument.StructuredArguments;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Aspect
public class LoggingAspect {

    private final SensitiveDataMasker masker;

    public LoggingAspect(SensitiveDataMasker masker) {
        this.masker = masker;
    }

    /**
     * Bắt tất cả method được annotate @Loggable ở bất kỳ class nào.
     */
    @Around("@annotation(loggable)")
    public Object logBusinessEvent(ProceedingJoinPoint pjp,
                                   Loggable loggable) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        List<StructuredArgument> baseArgs = buildBaseArgs(loggable, sig, pjp.getArgs());

        // Log khi bắt đầu method
        if (loggable.logOnEntry()) {
            List<StructuredArgument> entryArgs = new ArrayList<>(baseArgs);
            entryArgs.add(StructuredArguments.kv("phase", "START"));
            log.info(loggable.message() + " started", entryArgs.toArray());
        }

        long startTime = System.currentTimeMillis();

        try {
            Object result = pjp.proceed();
            long duration = System.currentTimeMillis() - startTime;

            // Log khi hoàn tất thành công
            List<StructuredArgument> successArgs = new ArrayList<>(baseArgs);
            successArgs.add(StructuredArguments.kv("phase", "SUCCESS"));
            successArgs.add(StructuredArguments.kv("duration_ms", duration));

            if (loggable.logReturnValue() && result != null) {
                successArgs.add(StructuredArguments.kv("result",
                    masker.maskObject(result)));
            }

            switch (loggable.successLevel()) {
                case DEBUG -> log.debug(loggable.message() + " completed", successArgs.toArray());
                case WARN  -> log.warn(loggable.message() + " completed", successArgs.toArray());
                default    -> log.info(loggable.message() + " completed", successArgs.toArray());
            }

            return result;

        } catch (Throwable ex) {
            long duration = System.currentTimeMillis() - startTime;

            // Log khi có exception
            List<StructuredArgument> errorArgs = new ArrayList<>(baseArgs);
            errorArgs.add(StructuredArguments.kv("phase", "ERROR"));
            errorArgs.add(StructuredArguments.kv("duration_ms", duration));
            errorArgs.add(StructuredArguments.kv("exception.type", ex.getClass().getSimpleName()));
            errorArgs.add(StructuredArguments.kv("exception.message", ex.getMessage()));

            log.error(loggable.message() + " failed", errorArgs.toArray(), ex);
            throw ex;   // re-throw để business logic xử lý bình thường
        }
    }

    /**
     * Xây dựng danh sách argument cơ bản: event, message, clientIp,
     * và các @LogParam từ method signature.
     */
    private List<StructuredArgument> buildBaseArgs(Loggable loggable,
                                                   MethodSignature sig,
                                                   Object[] args) {
        List<StructuredArgument> result = new ArrayList<>();
        result.add(StructuredArguments.kv("event", loggable.event()));

        // Thêm clientIp từ MDC (đã được MdcFilter set từ HTTP request)
        String clientIp = MDC.get("clientIp");
        if (clientIp != null) {
            result.add(StructuredArguments.kv("clientIp", clientIp));
        }

        // Đọc @LogParam từ từng parameter
        Parameter[] params = sig.getMethod().getParameters();
        for (int i = 0; i < params.length; i++) {
            LogParam logParam = params[i].getAnnotation(LogParam.class);
            if (logParam == null || args[i] == null) continue;

            String fieldName = logParam.value();
            String fieldValue = logParam.sensitive()
                ? "***"
                : masker.maskString(String.valueOf(args[i]));

            result.add(StructuredArguments.kv(fieldName, fieldValue));
        }

        return result;
    }
}
```

### 6.8. `SensitiveDataMasker.java` — Che giấu dữ liệu nhạy cảm

```java
package com.hotelhub.chassis.logging.util;

import com.hotelhub.chassis.logging.LoggingProperties;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class SensitiveDataMasker {

    private final LoggingProperties properties;

    // Pattern cứng cho các loại dữ liệu nhạy cảm biết trước
    private static final List<Pattern> HARDCODED_PATTERNS = List.of(
        // Số thẻ tín dụng (13-16 chữ số, có thể có dấu cách/gạch ngang)
        Pattern.compile("\\b(?:\\d[ -]?){13,16}\\b"),
        // Bearer token
        Pattern.compile("(?i)bearer\\s+[A-Za-z0-9\\-._~+/]+=*"),
        // VNPay secure hash
        Pattern.compile("(?i)vnp_SecureHash=[A-Fa-f0-9]+")
    );

    /**
     * Mask các giá trị nhạy cảm trong một chuỗi string.
     * Dùng cho log message tự do.
     */
    public String maskString(String input) {
        if (input == null) return null;

        String result = input;

        // Mask theo pattern cứng
        for (Pattern pattern : HARDCODED_PATTERNS) {
            result = pattern.matcher(result).replaceAll(m -> maskValue(m.group()));
        }

        // Mask theo field name được cấu hình trong properties
        for (String sensitiveField : properties.getSensitiveFields()) {
            Pattern fieldPattern = Pattern.compile(
                "(?i)(\"?" + Pattern.quote(sensitiveField) + "\"?\\s*[:=]\\s*\"?)([^\"&\\s]+)(\"?)",
                Pattern.CASE_INSENSITIVE
            );
            result = fieldPattern.matcher(result)
                .replaceAll(m -> m.group(1) + "***" + m.group(3));
        }

        return result;
    }

    /**
     * Mask object (dùng khi logReturnValue = true trong @Loggable).
     * Convert toString() rồi mask.
     */
    public String maskObject(Object obj) {
        return maskString(obj.toString());
    }

    private String maskValue(String value) {
        if (value.length() <= 4) return "***";
        // Giữ 2 ký tự đầu và 2 ký tự cuối, mask phần giữa
        return value.substring(0, 2) + "***" + value.substring(value.length() - 2);
    }
}
```

### 6.9. `KafkaLoggingUtils.java` — Correlation qua Kafka

```java
package com.hotelhub.chassis.logging.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

public final class KafkaLoggingUtils {

    // Header names phải khớp với MdcKafkaProducerInterceptor
    public static final String HEADER_TRACE_ID  = "X-Trace-Id";
    public static final String HEADER_TENANT_ID = "X-Tenant-Id";
    public static final String HEADER_USER_ID   = "X-User-Id";

    private KafkaLoggingUtils() {}

    /**
     * Gọi ở đầu mỗi @KafkaListener để setup MDC từ Kafka message header.
     * Giữ correlation với request gốc đã tạo ra message này.
     *
     * Luôn gọi clearMdc() trong finally block tương ứng.
     *
     * Ví dụ:
     * <pre>
     *   @KafkaListener(topics = "booking.created")
     *   public void handle(ConsumerRecord<String, BookingEvent> record) {
     *       KafkaLoggingUtils.setupMdcFromRecord(record);
     *       try {
     *           // business logic
     *       } finally {
     *           KafkaLoggingUtils.clearMdc();
     *       }
     *   }
     * </pre>
     */
    public static void setupMdcFromRecord(ConsumerRecord<?, ?> record) {
        try {
            MDC.put("traceId",  extractHeader(record, HEADER_TRACE_ID)
                .orElseGet(() -> UUID.randomUUID().toString().replace("-", "")));
            MDC.put("tenantId", extractHeader(record, HEADER_TENANT_ID)
                .orElse("platform"));
            MDC.put("userId",   extractHeader(record, HEADER_USER_ID)
                .orElse("system"));

            // Kafka-specific context
            MDC.put("kafka.topic",     record.topic());
            MDC.put("kafka.partition", String.valueOf(record.partition()));
            MDC.put("kafka.offset",    String.valueOf(record.offset()));
            MDC.put("kafka.key",       record.key() != null ? String.valueOf(record.key()) : "");
        } catch (Exception e) {
            // Không để lỗi setup MDC làm hỏng business logic
        }
    }

    /**
     * Xóa MDC sau khi xử lý xong Kafka message.
     * Bắt buộc gọi trong finally block.
     */
    public static void clearMdc() {
        MDC.clear();
    }

    private static Optional<String> extractHeader(ConsumerRecord<?, ?> record,
                                                   String headerName) {
        Header header = record.headers().lastHeader(headerName);
        if (header == null || header.value() == null) return Optional.empty();
        String value = new String(header.value(), StandardCharsets.UTF_8);
        return value.isBlank() ? Optional.empty() : Optional.of(value);
    }
}
```

### 6.10. `MdcKafkaProducerInterceptor.java` — Gắn traceId vào Kafka message

```java
package com.hotelhub.chassis.logging.kafka;

import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

public class MdcKafkaProducerInterceptor<K, V> implements ProducerInterceptor<K, V> {

    @Override
    public ProducerRecord<K, V> onSend(ProducerRecord<K, V> record) {
        // Propagate MDC context vào Kafka message header
        // để consumer ở service khác có thể tiếp tục chuỗi correlation
        addHeaderIfPresent(record, "X-Trace-Id",  MDC.get("traceId"));
        addHeaderIfPresent(record, "X-Tenant-Id", MDC.get("tenantId"));
        addHeaderIfPresent(record, "X-User-Id",   MDC.get("userId"));
        return record;
    }

    private void addHeaderIfPresent(ProducerRecord<K, V> record,
                                    String headerName, String value) {
        if (value != null && !value.isBlank()) {
            record.headers().add(headerName, value.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {}

    @Override
    public void close() {}

    @Override
    public void configure(Map<String, ?> configs) {}
}
```

### 6.11. `LoggingAutoConfiguration.java` — Kết nối tất cả lại

```java
package com.hotelhub.chassis.logging;

import com.hotelhub.chassis.logging.aop.LoggingAspect;
import com.hotelhub.chassis.logging.filter.MdcFilter;
import com.hotelhub.chassis.logging.util.SensitiveDataMasker;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnProperty(
    name = "chassis.logging.enabled",
    havingValue = "true",
    matchIfMissing = true   // Mặc định bật nếu không cấu hình
)
@EnableConfigurationProperties(LoggingProperties.class)
public class LoggingAutoConfiguration {

    @Bean
    public SensitiveDataMasker sensitiveDataMasker(LoggingProperties properties) {
        return new SensitiveDataMasker(properties);
    }

    /**
     * MdcFilter: chỉ tạo nếu là web application (có Servlet)
     */
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public MdcFilter mdcFilter(LoggingProperties properties) {
        return new MdcFilter(properties);
    }

    /**
     * LoggingAspect: chỉ tạo nếu AOP được bật trong properties
     */
    @Bean
    @ConditionalOnProperty(
        name = "chassis.logging.aspect-enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    @ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
    public LoggingAspect loggingAspect(SensitiveDataMasker masker) {
        return new LoggingAspect(masker);
    }
}
```

`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:
```
com.hotelhub.chassis.logging.LoggingAutoConfiguration
```

---

## 7. Hướng dẫn sử dụng tại các service

### 7.1. Thêm dependency

```xml
<!-- Trong pom.xml của từng service -->
<dependency>
    <groupId>com.hotelhub</groupId>
    <artifactId>hotelhub-chassis</artifactId>
    <version>1.0.0</version>
</dependency>
```

Không cần config gì thêm. Chassis tự hoạt động.

### 7.2. Cấu hình `application.yml` (tuỳ chọn override)

```yaml
spring:
  application:
    name: booking-service   # Service name xuất hiện trong mọi log

# Cấu hình chassis (tất cả đều có giá trị mặc định, không bắt buộc)
chassis:
  logging:
    enabled: true
    json-enabled: true          # false cho local dev (plain text)
    aspect-enabled: true        # bật @Loggable annotation
    slow-request-threshold-ms: 1000
    exclude-paths:
      - /actuator/health
      - /actuator/info
      - /actuator/prometheus

logging:
  level:
    root: INFO
    com.hotelhub: DEBUG         # Bật DEBUG cho code của dự án
```

### 7.3. Sử dụng `@Loggable` trong service

```java
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;

    // ── Đặt phòng ─────────────────────────────────────────────────────────
    @Loggable(event = "BOOKING_CREATED", message = "Create a booking")
    public BookingResponse createBooking(
            @LogParam("customerId") String customerId,
            @LogParam("hotelId")    String hotelId,
            @LogParam("roomTypeId") String roomTypeId,
            BookingRequest request) {
        // Không cần viết log thủ công
        // Aspect tự log: traceId, clientIp, customerId, hotelId, roomTypeId, duration
        return bookingRepository.save(mapToEntity(request));
    }

    // ── Hủy phòng ─────────────────────────────────────────────────────────
    @Loggable(event = "BOOKING_CANCELLED", message = "Cancel a booking")
    public void cancelBooking(
            @LogParam("bookingId") String bookingId,
            @LogParam("reason")    String reason) {
        bookingRepository.updateStatus(bookingId, BookingStatus.CANCELLED, reason);
    }

    // ── Check-in ──────────────────────────────────────────────────────────
    @Loggable(event = "GUEST_CHECKED_IN", message = "Check in guest")
    public void checkIn(
            @LogParam("bookingId") String bookingId,
            @LogParam("staffId")   String staffId) {
        bookingRepository.updateStatus(bookingId, BookingStatus.CHECKED_IN);
    }

    // ── Check-out ─────────────────────────────────────────────────────────
    @Loggable(event = "GUEST_CHECKED_OUT", message = "Check out guest")
    public void checkOut(
            @LogParam("bookingId") String bookingId,
            @LogParam("staffId")   String staffId) {
        bookingRepository.updateStatus(bookingId, BookingStatus.COMPLETED);
    }
}
```

```java
@Service
public class UserService {

    @Loggable(event = "USER_REGISTERED", message = "Add new user")
    public UserResponse registerUser(
            @LogParam("email") String email,
            @LogParam("role")  String role,
            @LogParam(value = "password", sensitive = true) String password,
            RegisterRequest request) {
        // password có sensitive = true → log thấy "***" thay vì giá trị thật
        return createUser(request);
    }

    @Loggable(event = "ACCOUNT_LOCKED", message = "Lock user account",
              successLevel = Loggable.LogLevel.WARN)
    public void lockAccount(
            @LogParam("targetUserId") String targetUserId,
            @LogParam("adminId")      String adminId,
            @LogParam("reason")       String reason) {
        userRepository.updateStatus(targetUserId, UserStatus.LOCKED);
    }
}
```

```java
@Service
public class PaymentService {

    @Loggable(event = "PAYMENT_INITIATED", message = "Initiate payment")
    public PaymentResult initiatePayment(
            @LogParam("bookingId") String bookingId,
            @LogParam("amount")    Long amount,
            @LogParam(value = "cardNumber", sensitive = true) String cardNumber) {
        return vnpayClient.createPayment(bookingId, amount);
    }

    @Loggable(event = "PAYMENT_SUCCESS", message = "Payment success")
    public void handlePaymentSuccess(
            @LogParam("bookingId")      String bookingId,
            @LogParam("transactionId")  String transactionId,
            @LogParam("amount")         Long amount) {
        // Aspect log: clientIp, bookingId, transactionId, amount, duration
        updateBookingStatus(bookingId, PaymentStatus.SUCCESS);
    }

    @Loggable(event = "PAYMENT_FAILED", message = "Payment failed",
              successLevel = Loggable.LogLevel.WARN)
    public void handlePaymentFailed(
            @LogParam("bookingId") String bookingId,
            @LogParam("reason")    String reason) {
        updateBookingStatus(bookingId, PaymentStatus.FAILED);
    }

    @Loggable(event = "REFUND_PROCESSED", message = "Process refund")
    public void processRefund(
            @LogParam("bookingId") String bookingId,
            @LogParam("amount")    Long amount) {
        vnpayClient.refund(bookingId, amount);
    }
}
```

```java
@Service
public class HotelService {

    @Loggable(event = "HOTEL_SUBMITTED", message = "Submit hotel listing for review")
    public HotelResponse createHotel(
            @LogParam("ownerId")   String ownerId,
            @LogParam("tenantId")  String tenantId,
            HotelRequest request) {
        return hotelRepository.save(mapToEntity(request));
    }

    @Loggable(event = "HOTEL_APPROVED", message = "Approve hotel listing")
    public void approveHotel(
            @LogParam("hotelId") String hotelId,
            @LogParam("adminId") String adminId) {
        hotelRepository.updateStatus(hotelId, HotelStatus.APPROVED);
    }

    @Loggable(event = "HOTEL_SUSPENDED", message = "Suspend hotel listing",
              successLevel = Loggable.LogLevel.WARN)
    public void suspendHotel(
            @LogParam("hotelId") String hotelId,
            @LogParam("adminId") String adminId,
            @LogParam("reason")  String reason) {
        hotelRepository.updateStatus(hotelId, HotelStatus.SUSPENDED);
    }

    @Loggable(event = "HOTEL_IMAGE_DELETED", message = "Delete hotel image")
    public void deleteImage(
            @LogParam("imageId")  String imageId,
            @LogParam("hotelId")  String hotelId,
            @LogParam("adminId")  String adminId,
            @LogParam("reason")   String reason) {
        minioService.delete(imageId);
        imageRepository.softDelete(imageId);
    }
}
```

### 7.4. Sử dụng Kafka logging

```java
@Component
public class BookingEventConsumer {

    @KafkaListener(topics = "payment.completed", groupId = "booking-service")
    public void handlePaymentCompleted(
            ConsumerRecord<String, PaymentCompletedEvent> record) {

        // Setup MDC từ Kafka header — giữ correlation với request gốc
        KafkaLoggingUtils.setupMdcFromRecord(record);

        try {
            PaymentCompletedEvent event = record.value();
            log.info("Processing payment completed event",
                StructuredArguments.kv("event", "PAYMENT_EVENT_CONSUMED"),
                StructuredArguments.kv("bookingId", event.getBookingId()),
                StructuredArguments.kv("transactionId", event.getTransactionId()));

            bookingService.confirmBooking(event.getBookingId());

        } catch (Exception ex) {
            log.error("Failed to process payment completed event",
                StructuredArguments.kv("event", "PAYMENT_EVENT_PROCESSING_FAILED"),
                ex);
            throw ex;

        } finally {
            // Bắt buộc: clear MDC để không leak sang message tiếp theo
            KafkaLoggingUtils.clearMdc();
        }
    }
}
```

### 7.5. Log thủ công khi cần thêm context

```java
// Dùng StructuredArguments thay vì string concatenation
// Lý do: string concatenation build string dù level bị tắt
// log.debug("User: " + userId)  ← tệ, build string kể cả khi DEBUG tắt
// log.debug("User: {}", userId) ← tốt hơn, không build khi tắt
// log.debug("User info", kv("userId", userId)) ← tốt nhất, structured JSON

import static net.logstash.logback.argument.StructuredArguments.kv;

log.info("Room reserved successfully",
    kv("event", "ROOM_RESERVED"),
    kv("roomTypeId", roomTypeId),
    kv("quantity", quantity),
    kv("checkIn", checkIn),
    kv("checkOut", checkOut));

log.warn("Low room availability detected",
    kv("event", "LOW_AVAILABILITY_WARNING"),
    kv("roomTypeId", roomTypeId),
    kv("availableRooms", available),
    kv("threshold", 3));
```

### 7.6. Output mẫu trong log

**Khi `@Loggable` method bắt đầu (phase START):**
```json
{
  "@timestamp":  "2026-08-03T10:30:15.123Z",
  "severity":    "INFO",
  "service":     "booking-service",
  "message":     "Create a booking started",
  "event":       "BOOKING_CREATED",
  "phase":       "START",
  "traceId":     "abc123def456789",
  "tenantId":    "tenant-001",
  "userId":      "user-123",
  "clientIp":    "113.185.54.22",
  "customerId":  "user-123",
  "hotelId":     "hotel-456",
  "roomTypeId":  "rt-789",
  "http.method": "POST",
  "http.path":   "/api/place-booking"
}
```

**Khi method hoàn tất (phase SUCCESS):**
```json
{
  "@timestamp":  "2026-08-03T10:30:15.268Z",
  "severity":    "INFO",
  "service":     "booking-service",
  "message":     "Create a booking completed",
  "event":       "BOOKING_CREATED",
  "phase":       "SUCCESS",
  "traceId":     "abc123def456789",
  "tenantId":    "tenant-001",
  "userId":      "user-123",
  "clientIp":    "113.185.54.22",
  "customerId":  "user-123",
  "hotelId":     "hotel-456",
  "roomTypeId":  "rt-789",
  "duration_ms": 145
}
```

**Khi method throw exception (phase ERROR):**
```json
{
  "@timestamp":       "2026-08-03T10:30:15.300Z",
  "severity":         "ERROR",
  "service":          "booking-service",
  "message":          "Create a booking failed",
  "event":            "BOOKING_CREATED",
  "phase":            "ERROR",
  "traceId":          "abc123def456789",
  "clientIp":         "113.185.54.22",
  "hotelId":          "hotel-456",
  "duration_ms":      177,
  "exception.type":   "RoomNotAvailableException",
  "exception.message":"No available rooms for roomTypeId=rt-789",
  "stack_trace":      "com.hotelhub.hotel.exception.RoomNotAvailableException: ..."
}
```

---

## 8. Cấu hình hạ tầng ELK Stack

### 8.1. `docker-compose.yml` — Thêm ELK services

```yaml
services:
  # ── Elasticsearch ──────────────────────────────────────────────────────
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.13.0
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false      # Tắt auth cho dev
      - ES_JAVA_OPTS=-Xms1g -Xmx1g       # Giới hạn RAM
    ports:
      - "9200:9200"
    volumes:
      - elasticsearch_data:/usr/share/elasticsearch/data
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9200/_cluster/health"]
      interval: 30s
      timeout: 10s
      retries: 5
    networks:
      - hotelhub-net

  # ── Kibana ─────────────────────────────────────────────────────────────
  kibana:
    image: docker.elastic.co/kibana/kibana:8.13.0
    environment:
      - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
    ports:
      - "5601:5601"
    depends_on:
      elasticsearch:
        condition: service_healthy
    networks:
      - hotelhub-net

  # ── Logstash ───────────────────────────────────────────────────────────
  logstash:
    image: docker.elastic.co/logstash/logstash:8.13.0
    volumes:
      - ./infra/logstash/pipeline:/usr/share/logstash/pipeline:ro
      - ./infra/logstash/config/logstash.yml:/usr/share/logstash/config/logstash.yml:ro
    ports:
      - "5044:5044"    # Beats input
      - "9600:9600"    # Logstash API
    depends_on:
      elasticsearch:
        condition: service_healthy
    networks:
      - hotelhub-net

  # ── Filebeat ───────────────────────────────────────────────────────────
  # Chạy trên mỗi Docker host, đọc log từ tất cả container
  filebeat:
    image: docker.elastic.co/beats/filebeat:8.13.0
    user: root
    volumes:
      - ./infra/filebeat/filebeat.yml:/usr/share/filebeat/filebeat.yml:ro
      - /var/lib/docker/containers:/var/lib/docker/containers:ro
      - /var/run/docker.sock:/var/run/docker.sock:ro
    depends_on:
      - logstash
    networks:
      - hotelhub-net

volumes:
  elasticsearch_data:

networks:
  hotelhub-net:
    driver: bridge
```

### 8.2. `filebeat.yml` — Đọc container logs

```yaml
# infra/filebeat/filebeat.yml
filebeat.inputs:
  - type: container
    paths:
      - '/var/lib/docker/containers/*/*.log'
    # Chỉ đọc log từ container có label hotelhub=true
    processors:
      - add_docker_metadata:
          host: "unix:///var/run/docker.sock"
      # Parse JSON log
      - decode_json_fields:
          fields: ["message"]
          target: ""
          overwrite_keys: true

output.logstash:
  hosts: ["logstash:5044"]

# Ghi lại vị trí đọc để không đọc lại sau khi restart
filebeat.registry.path: /usr/share/filebeat/data/registry
```

### 8.3. `logstash.conf` — Pipeline xử lý và routing

```ruby
# infra/logstash/pipeline/hotelhub.conf
input {
  beats {
    port => 5044
  }
}

filter {
  # Bỏ qua nếu không phải JSON hợp lệ (ví dụ: startup message của JVM)
  if ![service] {
    drop {}
  }

  # Đổi timestamp sang đúng format Elasticsearch
  date {
    match  => ["@timestamp", "ISO8601"]
    target => "@timestamp"
  }

  # Xóa field thừa từ Docker metadata
  mutate {
    remove_field => ["agent", "ecs", "input", "log", "host"]
  }

  # Thêm môi trường vào mọi document
  mutate {
    add_field => {
      "environment" => "${ENVIRONMENT:development}"
    }
  }
}

output {
  elasticsearch {
    hosts     => ["elasticsearch:9200"]
    # Index riêng theo service và ngày → dễ quản lý retention
    index     => "hotelhub-logs-%{[service]}-%{+yyyy.MM.dd}"
    # Template mapping để Elasticsearch biết kiểu dữ liệu của từng field
    template  => "/usr/share/logstash/templates/hotelhub.json"
    template_name => "hotelhub-logs"
    template_overwrite => true
  }
}
```

### 8.4. Index template cho Elasticsearch

```json
// infra/logstash/templates/hotelhub.json
{
  "index_patterns": ["hotelhub-logs-*"],
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0,
    "index.lifecycle.name": "hotelhub-logs-policy",
    "refresh_interval": "5s"
  },
  "mappings": {
    "properties": {
      "@timestamp":       { "type": "date" },
      "severity":         { "type": "keyword" },
      "service":          { "type": "keyword" },
      "traceId":          { "type": "keyword" },
      "spanId":           { "type": "keyword" },
      "tenantId":         { "type": "keyword" },
      "userId":           { "type": "keyword" },
      "clientIp":         { "type": "ip" },
      "event":            { "type": "keyword" },
      "message":          { "type": "text", "fields": { "raw": { "type": "keyword" }}},
      "duration_ms":      { "type": "long" },
      "http.method":      { "type": "keyword" },
      "http.path":        { "type": "keyword" },
      "http.status":      { "type": "integer" },
      "exception.type":   { "type": "keyword" },
      "exception.message":{ "type": "text" },
      "environment":      { "type": "keyword" },
      "kafka.topic":      { "type": "keyword" },
      "kafka.offset":     { "type": "long" }
    }
  }
}
```

### 8.5. Index Lifecycle Policy — Tự động xóa log cũ

```json
// Tạo qua Kibana Dev Tools hoặc Elasticsearch API
PUT _ilm/policy/hotelhub-logs-policy
{
  "policy": {
    "phases": {
      "hot": {
        "actions": {
          "rollover": {
            "max_size": "5gb",
            "max_age": "1d"
          }
        }
      },
      "warm": {
        "min_age": "7d",
        "actions": {
          "shrink":    { "number_of_shards": 1 },
          "forcemerge":{ "max_num_segments": 1 }
        }
      },
      "delete": {
        "min_age": "30d",
        "actions": { "delete": {} }
      }
    }
  }
}
```

---

## 9. Best practices

### 9.1. Về nội dung log

```
✅ Log sự kiện nghiệp vụ quan trọng với @Loggable
✅ Dùng UPPER_SNAKE_CASE cho event name: "BOOKING_CREATED" không phải "bookingCreated"
✅ Dùng tên field nhất quán toàn hệ thống: bookingId, hotelId, userId
✅ Log state transition: "Booking status changed PENDING → CONFIRMED"
✅ Log exception với StructuredArguments, không dùng log.error("Error: " + ex.getMessage())
✅ Dùng kv() không phải string interpolation cho structured log

❌ Không log trong vòng lặp (gây volume explosion)
❌ Không log mỗi lần đọc DB thành công (chỉ log khi lỗi hoặc bất thường)
❌ Không log dữ liệu nhạy cảm — dùng @LogParam(sensitive = true)
❌ Không dùng "log.debug("Value: " + value)" — build string dù DEBUG tắt
❌ Không bắt Exception chỉ để log rồi re-throw mà không thêm context mới
```

### 9.2. Về operational

```
✅ Set retention policy 30 ngày cho production log
✅ Tạo sẵn saved search trên Kibana: "All errors last 1h", "Errors by service"
✅ Monitor volume log — đột biến volume là dấu hiệu có vấn đề
✅ Tạo Kibana alert khi ERROR count tăng vượt ngưỡng

❌ Không dùng chung index cho tất cả service (gây khó query và retention phức tạp)
❌ Không để log accumulate không giới hạn (disk sẽ đầy)
❌ Không log ở level DEBUG trong production mà không có cơ chế tắt tự động
```

### 9.3. Về @Loggable

```
✅ Annotate ở Service layer, không phải Controller hay Repository
✅ Chọn event name mô tả hành động, không phải tên method
✅ @LogParam cho các ID và business entity, không phải toàn bộ object
✅ Dùng sensitive = true cho bất kỳ field nào có thể chứa dữ liệu nhạy cảm

❌ Không annotate method gọi trong vòng lặp
❌ Không logReturnValue = true nếu response chứa dữ liệu nhạy cảm
❌ Không @Loggable trên private method — AOP không bắt được
```

---

## 10. Khả năng thay đổi log stack

### 10.1. Nguyên tắc thiết kế cho khả năng thay stack

Toàn bộ chassis được thiết kế theo nguyên tắc: **code service không biết log đi đâu**. Điều này đạt được nhờ:

1. **Service chỉ ghi ra stdout.** Không có dependency nào vào Logstash, Elasticsearch hay bất kỳ log backend nào.
2. **Format JSON chuẩn.** Schema log được thiết kế tương thích với nhiều backend (ELK, Loki, CloudWatch, Datadog...).
3. **Transport layer hoàn toàn ở hạ tầng.** Filebeat, Logstash, OTel Collector là các component hạ tầng, không phải code service.

### 10.2. Kịch bản: Chuyển sang OTel + Loki + Grafana

Khi muốn chuyển stack (ví dụ: ELK tốn quá nhiều RAM ở production), chỉ cần:

**Bước 1:** Không cần sửa bất kỳ dòng code nào trong service hoặc chassis.

**Bước 2:** Thay Filebeat + Logstash bằng OTel Collector trong Docker Compose:

```yaml
# Xóa filebeat và logstash
# Thay bằng:
otel-collector:
  image: otel/opentelemetry-collector-contrib:0.100.0
  volumes:
    - ./infra/otel/otel-collector.yaml:/etc/otel/config.yaml
  command: ["--config=/etc/otel/config.yaml"]

loki:
  image: grafana/loki:3.0.0
  ports:
    - "3100:3100"

grafana:
  image: grafana/grafana:11.0.0
  environment:
    - GF_AUTH_ANONYMOUS_ENABLED=true
  ports:
    - "3000:3000"
```

**Bước 3:** Viết OTel Collector config đọc stdout và forward đến Loki:

```yaml
# infra/otel/otel-collector.yaml
receivers:
  filelog:
    include: ['/var/lib/docker/containers/*/*.log']
    operators:
      - type: json_parser
        timestamp:
          parse_from: attributes["@timestamp"]
          layout: '%Y-%m-%dT%H:%M:%S.%LZ'

processors:
  resource:
    attributes:
      - key: environment
        value: production
        action: insert

exporters:
  loki:
    endpoint: http://loki:3100/loki/api/v1/push
    labels:
      resource:
        service: ""        # lấy từ resource attribute "service"
        traceId: ""
        severity: ""

service:
  pipelines:
    logs:
      receivers: [filelog]
      processors: [resource]
      exporters: [loki]
```

**Tổng kết thay đổi:**

| Thành phần | ELK | OTel + Loki + Grafana |
| --- | --- | --- |
| Code service | Không đổi | Không đổi |
| Chassis module | Không đổi | Không đổi |
| logback-spring.xml | Không đổi | Không đổi |
| Filebeat | Xóa | Không có |
| Logstash | Xóa | Không có |
| Elasticsearch | Xóa | Không có |
| Kibana | Xóa | Không có |
| OTel Collector | Không có | Thêm |
| Loki | Không có | Thêm |
| Grafana | Không có | Thêm |

**Thời gian migration ước tính: 1–2 ngày cho hạ tầng, 0 ngày cho code.**

### 10.3. Cấu hình chassis để hỗ trợ override profile dễ hơn

Có thể thêm property để bật/tắt tính năng theo stack (tuy nhiên không thực sự cần thiết nếu đã thiết kế đúng):

```yaml
# application-elk.yml (profile cho ELK)
chassis:
  logging:
    json-enabled: true

# application-loki.yml (profile cho Loki)  
chassis:
  logging:
    json-enabled: true   # Không cần thay đổi — Loki đọc được JSON như ELK
```

Vì cả hai stack đều đọc JSON từ stdout, không cần thay đổi gì trong chassis hay service.

---

## Phụ lục: Danh sách event name chuẩn toàn hệ thống

| Service | Event | Mô tả |
| --- | --- | --- |
| User Service | `USER_REGISTERED` | Đăng ký tài khoản mới |
| User Service | `USER_LOGGED_IN` | Đăng nhập thành công |
| User Service | `GOOGLE_AUTH_SUCCESS` | Đăng nhập Google thành công |
| User Service | `PASSWORD_RESET_REQUESTED` | Yêu cầu đặt lại mật khẩu |
| User Service | `ACCOUNT_LOCKED` | Admin khóa tài khoản |
| User Service | `ACCOUNT_UNLOCKED` | Admin mở khóa tài khoản |
| Hotel Service | `HOTEL_SUBMITTED` | Chủ KS nộp tin đăng chờ duyệt |
| Hotel Service | `HOTEL_APPROVED` | Admin duyệt khách sạn |
| Hotel Service | `HOTEL_SUSPENDED` | Admin tạm ngưng khách sạn |
| Hotel Service | `HOTEL_IMAGE_DELETED` | Admin xóa ảnh vi phạm |
| Hotel Service | `ROOM_TYPE_CREATED` | Tạo loại phòng mới |
| Booking Service | `BOOKING_CREATED` | Tạo đơn đặt phòng |
| Booking Service | `BOOKING_CONFIRMED` | Xác nhận đặt phòng tự động |
| Booking Service | `BOOKING_CANCELLED` | Hủy đặt phòng |
| Booking Service | `GUEST_CHECKED_IN` | Khách check-in |
| Booking Service | `GUEST_CHECKED_OUT` | Khách check-out |
| Payment Service | `PAYMENT_INITIATED` | Khởi tạo giao dịch thanh toán |
| Payment Service | `PAYMENT_SUCCESS` | Thanh toán thành công |
| Payment Service | `PAYMENT_FAILED` | Thanh toán thất bại |
| Payment Service | `REFUND_PROCESSED` | Xử lý hoàn tiền |
| Place Booking Service | `SAGA_STARTED` | Bắt đầu saga đặt phòng |
| Place Booking Service | `SAGA_COMPLETED` | Saga hoàn tất thành công |
| Place Booking Service | `SAGA_COMPENSATING` | Bắt đầu rollback saga |
| Place Booking Service | `SAGA_FAILED` | Saga thất bại sau rollback |
| Promotion Service | `COUPON_APPLIED` | Áp dụng mã giảm giá |
| Promotion Service | `COUPON_RELEASED` | Hoàn lại lượt coupon (rollback) |
| Notification Service | `NOTIFICATION_SENT` | Gửi thông báo thành công |
| Notification Service | `NOTIFICATION_FAILED` | Gửi thông báo thất bại |

---

## 11. Tích hợp OpenTelemetry + Jaeger (Distributed Tracing)

### 11.1. Tại sao cần Distributed Tracing riêng biệt với Logging?

Log và Trace giải quyết hai vấn đề khác nhau, bổ sung lẫn nhau:

| | Centralized Logging (ELK) | Distributed Tracing (OTel + Jaeger) |
| --- | --- | --- |
| **Câu hỏi trả lời** | Chuyện gì đã xảy ra? | Request đi qua đâu và tốn bao lâu ở mỗi chỗ? |
| **Đơn vị dữ liệu** | Log line (sự kiện rời rạc) | Trace → Span (cây phân cấp có quan hệ) |
| **Dùng khi** | Debug lỗi cụ thể, tìm exception | Phân tích latency, tìm bottleneck, visualize luồng |
| **Liên kết** | `traceId` trong log → tìm trace tương ứng trên Jaeger |

Hai hệ thống liên kết với nhau qua `traceId`: từ một log có lỗi, có thể nhảy sang Jaeger để xem toàn bộ trace của request đó và ngược lại.

```
Developer thấy lỗi trên Kibana:
  → Copy traceId: "abc123"
  → Search trên Jaeger: traceId = abc123
  → Thấy ngay: Gateway(12ms) → PlaceBooking(8ms) → Payment(2100ms) → Timeout
  → Bottleneck nằm ở Payment Service
```

### 11.2. Kiến trúc OTel + Jaeger thêm vào hệ thống

```
┌────────────────────────────────────────────────────────────────┐
│                     Mỗi Microservice                           │
│                                                                │
│  [OTel Java Agent] ─ auto-instrument Spring Boot, Kafka,      │
│                       JDBC, OpenFeign, RestTemplate            │
│         │                                                      │
│         ├── Traces ──────────────────────────────────────────► │
│         │                                                      │
│  [stdout JSON log] ── traceId/spanId từ OTel context ────────►│
└──────────────────────────────┬─────────────────────────────────┘
                               │ OTLP (gRPC port 4317)
                               ▼
                  ┌─────────────────────────┐
                  │   OTel Collector        │
                  │  (thay Logstash)        │
                  │                         │
                  │  receivers:             │
                  │    - otlp (traces)      │
                  │    - filelog (logs)     │
                  │                         │
                  │  exporters:             │
                  │    - jaeger (traces)    │
                  │    - elasticsearch (logs│
                  └───────────┬─────────────┘
                              │
               ┌──────────────┴──────────────┐
               ▼                             ▼
        ┌─────────────┐             ┌───────────────┐
        │   Jaeger    │             │ Elasticsearch │
        │  (traces)   │             │  (logs)       │
        └──────┬──────┘             └───────┬───────┘
               │                            │
               ▼                            ▼
        ┌─────────────────────────────────────────┐
        │              Jaeger UI / Kibana          │
        │   (liên kết qua traceId)                │
        └─────────────────────────────────────────┘
```

**Thay đổi so với kiến trúc cũ:**
- **Bỏ Logstash** → thay bằng **OTel Collector** (đảm nhận cả thu thập log lẫn traces qua một pipeline duy nhất)
- **Bỏ Filebeat** → OTel Collector đọc trực tiếp container stdout
- **Thêm OTel Java Agent** → auto-instrument tất cả service không cần sửa code
- **Thêm Jaeger** → lưu trữ và visualize trace

### 11.3. Khái niệm cốt lõi của OTel Tracing

Trước khi cài đặt, cần hiểu rõ ba khái niệm:

**Trace:** Toàn bộ hành trình của một request từ đầu đến cuối, xuyên qua nhiều service. Được định danh bởi `traceId` duy nhất.

**Span:** Một đơn vị công việc trong trace. Mỗi service tạo ra một hoặc nhiều span. Span có `spanId` riêng và `parentSpanId` trỏ về span cha.

**Context Propagation:** Cơ chế truyền `traceId` + `spanId` từ service này sang service khác qua HTTP header (định dạng W3C TraceContext) hoặc Kafka message header. OTel Agent xử lý tự động.

```
Trace abc123
├── Span s1: API Gateway (12ms)
│   ├── Span s2: Place Booking Service (8ms)
│   │   ├── Span s3: Booking Service - create booking (3ms)
│   │   ├── Span s4: Hotel Service - reserve room (2ms)
│   │   └── Span s5: Payment Service - charge (2100ms) ← CHẬM
│   │       └── Span s6: VNPay HTTP call (2090ms) ← bottleneck
│   └── Span s7: Notification Service (5ms)
```

### 11.4. Thêm dependency vào Chassis Module

```xml
<!-- pom.xml của hotelhub-chassis -->

<!-- OTel API — dùng để tạo span thủ công khi cần -->
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-api</artifactId>
    <version>1.38.0</version>
</dependency>

<!-- Bridge OTel → SLF4J MDC: tự động đồng bộ traceId/spanId 
     từ OTel context vào MDC để log JSON có traceId đúng -->
<dependency>
    <groupId>io.opentelemetry.instrumentation</groupId>
    <artifactId>opentelemetry-logback-mdc-1.0</artifactId>
    <version>2.4.0-alpha</version>
    <scope>runtime</scope>
</dependency>

<!-- Spring Boot OTel auto-configuration -->
<dependency>
    <groupId>io.opentelemetry.springboot</groupId>
    <artifactId>spring-boot-starter-opentelemetry</artifactId>
    <version>2.4.0-alpha</version>
</dependency>
```

> **Lưu ý:** OTel Java Agent (`.jar` chạy qua `-javaagent`) là cách tiếp cận thứ hai, không cần thêm dependency vào code. Hai cách có thể dùng kết hợp hoặc chọn một. Tài liệu này dùng **SDK in-process** (dependency) để có thể tạo span tùy chỉnh và tích hợp với chassis AOP.

### 11.5. Cập nhật `logback-spring.xml` — Đồng bộ OTel context vào MDC

OTel SDK tự động set `traceId` và `spanId` vào MDC thông qua `OpenTelemetryAppender` hoặc bridge library. Cần thêm appender này vào cấu hình Logback để traceId trong log khớp chính xác với traceId trên Jaeger:

```xml
<!-- Thêm vào logback-spring.xml hiện có, trong phần <configuration> -->

<!-- OTel MDC bridge: tự động inject traceId, spanId từ OTel context vào MDC
     Cần đặt TRƯỚC các appender khác để traceId có sẵn khi encode JSON -->
<appender name="OTEL_BRIDGE"
          class="io.opentelemetry.instrumentation.logback.mdc.v1_0.OpenTelemetryAppender">
    <!-- Delegate sang appender thực sự -->
    <appender-ref ref="ASYNC_JSON"/>
</appender>

<!-- Cập nhật root logger trong profile staging/production: 
     thay ASYNC_JSON bằng OTEL_BRIDGE để traceId được inject trước khi log -->
<springProfile name="staging,production">
    <root level="${LOG_LEVEL_ROOT}">
        <appender-ref ref="OTEL_BRIDGE"/>  <!-- ← đổi từ ASYNC_JSON sang OTEL_BRIDGE -->
    </root>
</springProfile>
```

Với cách này, mọi dòng log JSON sẽ tự động có:
```json
{
  "traceId": "abc123def456789012345678901234",  ← từ OTel active span
  "spanId":  "abcdef1234567890",                ← từ OTel active span
  ...
}
```

Và traceId này **chính xác khớp** với traceId trên Jaeger UI — điều không thể đảm bảo nếu tự sinh traceId trong `MdcFilter`.

### 11.6. Cập nhật `MdcFilter` — Ưu tiên traceId từ OTel

Khi OTel Agent/SDK đang chạy, `MdcFilter` không nên tự sinh `traceId` nữa mà phải lấy từ OTel context để đảm bảo nhất quán:

```java
// Thêm vào MdcFilter.java trong chassis

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;

private void setupMdc(HttpServletRequest request) {

    // Lấy traceId từ OTel active span (nếu OTel SDK đang chạy)
    // OTel Agent tự tạo span trước khi MdcFilter chạy nếu cấu hình đúng
    SpanContext otelContext = Span.current().getSpanContext();

    String traceId;
    String spanId;

    if (otelContext.isValid()) {
        // OTel đang active — dùng traceId/spanId của OTel
        // Đảm bảo log và trace trên Jaeger có cùng traceId
        traceId = otelContext.getTraceId();   // 32 hex chars W3C format
        spanId  = otelContext.getSpanId();    // 16 hex chars
    } else {
        // Fallback: OTel không active (unit test, internal call...)
        // Lấy từ header hoặc tự sinh
        traceId = Optional.ofNullable(request.getHeader(TRACE_ID_HEADER))
            .filter(id -> !id.isBlank())
            .orElseGet(() -> UUID.randomUUID().toString().replace("-", ""));
        spanId = UUID.randomUUID().toString().substring(0, 16);
    }

    MDC.put(MDC_TRACE_ID,  traceId);
    MDC.put(MDC_SPAN_ID,   spanId);

    // Các field khác giữ nguyên như cũ
    MDC.put(MDC_TENANT_ID,  getHeaderOrDefault(request, TENANT_ID_HEADER, "platform"));
    MDC.put(MDC_USER_ID,    getHeaderOrDefault(request, USER_ID_HEADER, "anonymous"));
    MDC.put(MDC_CLIENT_IP,  extractClientIp(request));
    MDC.put(MDC_REQUEST_ID, UUID.randomUUID().toString());
    MDC.put(MDC_HTTP_METHOD, request.getMethod());
    MDC.put(MDC_HTTP_PATH,   request.getRequestURI());
}
```

### 11.7. Tạo Custom Span trong `LoggingAspect`

`@Loggable` AOP aspect hiện tại chỉ ghi log. Nâng cấp để **đồng thời tạo OTel span** cho mỗi business method, giúp Jaeger hiển thị chi tiết đến tầng service method (không chỉ ở tầng HTTP call):

```java
// Cập nhật LoggingAspect.java trong chassis

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

@Slf4j
@Aspect
public class LoggingAspect {

    private final SensitiveDataMasker masker;

    // Tracer lấy từ OTel SDK — nếu OTel không được cấu hình,
    // sẽ trả về NoopTracer (không làm gì, không throw exception)
    private final Tracer tracer = GlobalOpenTelemetry
        .getTracer("com.hotelhub.chassis", "1.1.0");

    public LoggingAspect(SensitiveDataMasker masker) {
        this.masker = masker;
    }

    @Around("@annotation(loggable)")
    public Object logBusinessEvent(ProceedingJoinPoint pjp,
                                   Loggable loggable) throws Throwable {

        MethodSignature sig = (MethodSignature) pjp.getSignature();
        List<StructuredArgument> baseArgs = buildBaseArgs(loggable, sig, pjp.getArgs());

        // Tạo span con để Jaeger hiển thị business method riêng
        // SpanKind.INTERNAL = xử lý nội bộ, không phải HTTP call
        Span span = tracer.spanBuilder(loggable.event())
            .setSpanKind(SpanKind.INTERNAL)
            .startSpan();

        // Gắn attribute vào span để có thể filter trên Jaeger
        span.setAttribute("business.event", loggable.event());
        span.setAttribute("business.message", loggable.message());
        span.setAttribute("service.layer", "business");

        // Thêm @LogParam vào span attribute
        Parameter[] params = sig.getMethod().getParameters();
        Object[] args = pjp.getArgs();
        for (int i = 0; i < params.length; i++) {
            LogParam logParam = params[i].getAnnotation(LogParam.class);
            if (logParam == null || args[i] == null) continue;
            if (!logParam.sensitive()) {
                span.setAttribute(logParam.value(), String.valueOf(args[i]));
            }
        }

        // Scope đảm bảo span này là "active span" trong thread hiện tại
        // → các span con tạo ra bên trong method sẽ tự động có parentSpanId = span này
        try (Scope scope = span.makeCurrent()) {

            if (loggable.logOnEntry()) {
                List<StructuredArgument> entryArgs = new ArrayList<>(baseArgs);
                entryArgs.add(StructuredArguments.kv("phase", "START"));
                log.info(loggable.message() + " started", entryArgs.toArray());
            }

            long startTime = System.currentTimeMillis();

            try {
                Object result = pjp.proceed();
                long duration = System.currentTimeMillis() - startTime;

                span.setAttribute("duration_ms", duration);
                span.setStatus(StatusCode.OK);

                List<StructuredArgument> successArgs = new ArrayList<>(baseArgs);
                successArgs.add(StructuredArguments.kv("phase", "SUCCESS"));
                successArgs.add(StructuredArguments.kv("duration_ms", duration));

                switch (loggable.successLevel()) {
                    case DEBUG -> log.debug(loggable.message() + " completed", successArgs.toArray());
                    case WARN  -> log.warn(loggable.message() + " completed", successArgs.toArray());
                    default    -> log.info(loggable.message() + " completed", successArgs.toArray());
                }

                return result;

            } catch (Throwable ex) {
                long duration = System.currentTimeMillis() - startTime;

                // Đánh dấu span là ERROR — Jaeger sẽ highlight màu đỏ
                span.setStatus(StatusCode.ERROR, ex.getMessage());
                span.recordException(ex);   // ghi stack trace vào span event
                span.setAttribute("duration_ms", duration);
                span.setAttribute("exception.type", ex.getClass().getSimpleName());

                List<StructuredArgument> errorArgs = new ArrayList<>(baseArgs);
                errorArgs.add(StructuredArguments.kv("phase", "ERROR"));
                errorArgs.add(StructuredArguments.kv("duration_ms", duration));
                errorArgs.add(StructuredArguments.kv("exception.type", ex.getClass().getSimpleName()));
                errorArgs.add(StructuredArguments.kv("exception.message", ex.getMessage()));
                log.error(loggable.message() + " failed", errorArgs.toArray(), ex);

                throw ex;
            }

        } finally {
            // Kết thúc span — bắt buộc phải gọi để span được export
            span.end();
        }
    }
}
```

Kết quả trên Jaeger UI:
```
Trace abc123 (total: 165ms)
├── [Gateway] HTTP POST /api/place-booking           (12ms)
│   └── [PlaceBooking] SAGA_STARTED                  (8ms)
│       ├── [Booking] BOOKING_CREATED                (3ms)   ← span từ @Loggable
│       ├── [Hotel] ROOM_RESERVED                    (2ms)   ← span từ @Loggable
│       └── [Payment] PAYMENT_INITIATED              (145ms) ← span từ @Loggable
│           └── [HTTP] POST https://sandbox.vnpay.vn (140ms) ← auto-instrument
```

### 11.8. Cập nhật `KafkaLoggingUtils` — Propagate OTel context qua Kafka

OTel Agent tự động inject/extract context vào Kafka message header khi dùng `spring-kafka`. Tuy nhiên, khi dùng SDK (không có Agent), cần xử lý thủ công:

```java
// Thêm vào KafkaLoggingUtils.java

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import org.apache.kafka.common.header.Headers;

public final class KafkaLoggingUtils {

    // W3C TraceContext propagator — format chuẩn cho traceId/spanId
    private static final TextMapGetter<Headers> KAFKA_GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(Headers headers) {
            return StreamSupport.stream(headers.spliterator(), false)
                .map(Header::key)
                .collect(Collectors.toList());
        }

        @Override
        public String get(Headers headers, String key) {
            Header header = headers.lastHeader(key);
            return header != null
                ? new String(header.value(), StandardCharsets.UTF_8)
                : null;
        }
    };

    /**
     * Gọi ở đầu @KafkaListener để:
     * 1. Restore OTel context từ Kafka message header (tiếp tục trace gốc)
     * 2. Setup MDC với traceId từ OTel context vừa restore
     *
     * Trả về Context scope — phải đóng trong finally:
     *   Scope scope = KafkaLoggingUtils.setupFromRecord(record);
     *   try { ... } finally { scope.close(); KafkaLoggingUtils.clearMdc(); }
     */
    public static io.opentelemetry.context.Scope setupFromRecord(
            ConsumerRecord<?, ?> record) {
        try {
            // Extract OTel context từ Kafka header (traceId + spanId của producer)
            Context extractedContext = GlobalOpenTelemetry.getPropagators()
                .getTextMapPropagator()
                .extract(Context.current(), record.headers(), KAFKA_GETTER);

            // Activate context → span mới tạo ra sẽ là con của trace gốc
            io.opentelemetry.context.Scope scope = extractedContext.makeCurrent();

            // Sync vào MDC để log có đúng traceId
            SpanContext spanCtx = Span.current().getSpanContext();
            if (spanCtx.isValid()) {
                MDC.put("traceId", spanCtx.getTraceId());
                MDC.put("spanId",  spanCtx.getSpanId());
            } else {
                // Fallback: đọc từ custom header (tương thích với cách cũ)
                setupMdcFromHeader(record);
            }

            // Kafka-specific context
            MDC.put("tenantId",        extractHeader(record, HEADER_TENANT_ID).orElse("platform"));
            MDC.put("userId",          extractHeader(record, HEADER_USER_ID).orElse("system"));
            MDC.put("kafka.topic",     record.topic());
            MDC.put("kafka.partition", String.valueOf(record.partition()));
            MDC.put("kafka.offset",    String.valueOf(record.offset()));

            return scope;

        } catch (Exception e) {
            return io.opentelemetry.context.Scope.noop();
        }
    }

    public static void clearMdc() {
        MDC.clear();
    }

    // ... các method helper giữ nguyên như cũ
}
```

**Cách dùng cập nhật trong Kafka consumer:**

```java
@KafkaListener(topics = "payment.completed", groupId = "booking-service")
public void handlePaymentCompleted(
        ConsumerRecord<String, PaymentCompletedEvent> record) {

    // Dùng setupFromRecord thay vì setupMdcFromRecord cũ
    try (io.opentelemetry.context.Scope scope =
             KafkaLoggingUtils.setupFromRecord(record)) {
        try {
            PaymentCompletedEvent event = record.value();
            // Log và trace đều có đúng traceId của request gốc tạo ra event này
            log.info("Processing payment completed",
                kv("event", "PAYMENT_EVENT_CONSUMED"),
                kv("bookingId", event.getBookingId()));

            bookingService.confirmBooking(event.getBookingId());

        } catch (Exception ex) {
            log.error("Failed to process payment event", ex);
            throw ex;
        } finally {
            KafkaLoggingUtils.clearMdc();
        }
    } // scope.close() tự động gọi ở đây
}
```

### 11.9. Cập nhật `LoggingAutoConfiguration` — Điều kiện cho OTel

```java
// Thêm vào LoggingAutoConfiguration.java

import io.opentelemetry.api.OpenTelemetry;

@Bean
@ConditionalOnClass(name = "io.opentelemetry.api.GlobalOpenTelemetry")
@ConditionalOnProperty(
    name = "chassis.logging.tracing-enabled",
    havingValue = "true",
    matchIfMissing = true
)
public LoggingAspect loggingAspect(SensitiveDataMasker masker) {
    // LoggingAspect giờ tự lấy Tracer từ GlobalOpenTelemetry
    // Nếu OTel không được configure → NoopTracer → không có span nhưng vẫn có log
    return new LoggingAspect(masker);
}
```

Thêm property vào `LoggingProperties`:
```java
/** Bật tạo OTel span trong @Loggable aspect.
 *  Tắt nếu không dùng OTel (giảm overhead). */
private boolean tracingEnabled = true;
```

### 11.10. Cấu hình `application.yml` cho OTel + Jaeger

```yaml
# Trong application.yml của từng service (hoặc trong chassis default config)

spring:
  application:
    name: booking-service

# OTel SDK configuration — Spring Boot auto-configuration đọc prefix này
management:
  tracing:
    sampling:
      probability: 1.0   # 100% sampling cho dev; giảm xuống 0.1 ở production

otel:
  service:
    name: ${spring.application.name}

  # Export traces đến OTel Collector (không gửi thẳng đến Jaeger)
  # Collector sẽ forward đến Jaeger — tách hạ tầng khỏi service
  exporter:
    otlp:
      endpoint: http://otel-collector:4317
      protocol: grpc

  # Propagation format: W3C TraceContext (interoperable với Jaeger, Zipkin, Datadog)
  propagators: tracecontext,baggage

  # Tắt các metric exporter mặc định (chúng ta dùng Prometheus riêng)
  metrics:
    exporter: none

  # Log exporter: none — log được handle bởi Logback/OTel Collector pipeline
  logs:
    exporter: none

chassis:
  logging:
    enabled: true
    json-enabled: true
    tracing-enabled: true
    slow-request-threshold-ms: 1000
```

### 11.11. Thay Logstash bằng OTel Collector — Cập nhật Docker Compose

```yaml
# docker-compose.yml — phần infrastructure

services:

  # ── OTel Collector (thay thế Filebeat + Logstash) ─────────────────────
  otel-collector:
    image: otel/opentelemetry-collector-contrib:0.100.0
    command: ["--config=/etc/otel/config.yaml"]
    volumes:
      - ./infra/otel/otel-collector.yaml:/etc/otel/config.yaml:ro
      # Mount Docker socket để đọc container logs
      - /var/run/docker.sock:/var/run/docker.sock:ro
      - /var/lib/docker/containers:/var/lib/docker/containers:ro
    ports:
      - "4317:4317"    # OTLP gRPC — service gửi traces/metrics đến đây
      - "4318:4318"    # OTLP HTTP — alternative cho gRPC
      - "8888:8888"    # Collector internal metrics (Prometheus scrape)
    depends_on:
      - elasticsearch
      - jaeger
    networks:
      - hotelhub-net

  # ── Jaeger (Distributed Tracing backend) ──────────────────────────────
  jaeger:
    image: jaegertracing/all-in-one:1.57
    environment:
      - COLLECTOR_OTLP_ENABLED=true    # Nhận trace qua OTLP protocol
      - SPAN_STORAGE_TYPE=elasticsearch
      - ES_SERVER_URLS=http://elasticsearch:9200
      - ES_INDEX_PREFIX=jaeger
    ports:
      - "16686:16686"  # Jaeger UI
      - "14317:4317"   # OTLP gRPC (Jaeger nhận từ Collector)
      - "14318:4318"   # OTLP HTTP
    depends_on:
      elasticsearch:
        condition: service_healthy
    networks:
      - hotelhub-net

  # ── Elasticsearch (dùng chung cho cả logs và traces) ──────────────────
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.13.0
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - ES_JAVA_OPTS=-Xms1g -Xmx2g
    ports:
      - "9200:9200"
    volumes:
      - elasticsearch_data:/usr/share/elasticsearch/data
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9200/_cluster/health"]
      interval: 30s
      timeout: 10s
      retries: 5
    networks:
      - hotelhub-net

  # ── Kibana (visualize logs) ────────────────────────────────────────────
  kibana:
    image: docker.elastic.co/kibana/kibana:8.13.0
    environment:
      - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
    ports:
      - "5601:5601"
    depends_on:
      elasticsearch:
        condition: service_healthy
    networks:
      - hotelhub-net

  # ── Prometheus + Grafana (metrics) ─────────────────────────────────────
  prometheus:
    image: prom/prometheus:v2.52.0
    volumes:
      - ./infra/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro
    ports:
      - "9090:9090"
    networks:
      - hotelhub-net

  grafana:
    image: grafana/grafana:11.0.0
    environment:
      - GF_AUTH_ANONYMOUS_ENABLED=true
      - GF_AUTH_ANONYMOUS_ORG_ROLE=Admin
    volumes:
      - ./infra/grafana/provisioning:/etc/grafana/provisioning:ro
    ports:
      - "3000:3000"
    depends_on:
      - prometheus
      - jaeger
    networks:
      - hotelhub-net

volumes:
  elasticsearch_data:

networks:
  hotelhub-net:
    driver: bridge
```

> **Lưu ý về Jaeger storage:** Ví dụ trên dùng Elasticsearch làm storage backend cho Jaeger (`SPAN_STORAGE_TYPE=elasticsearch`). Cả log (Kibana) và trace (Jaeger) đều lưu trên cùng một Elasticsearch cluster, giúp tiết kiệm tài nguyên khi dev. Ở production nên tách riêng.

### 11.12. Cấu hình OTel Collector pipeline

```yaml
# infra/otel/otel-collector.yaml

receivers:
  # ── Nhận traces và metrics từ service qua OTLP ───────────────────────
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317
      http:
        endpoint: 0.0.0.0:4318

  # ── Đọc logs từ stdout của Docker containers (thay Filebeat) ─────────
  filelog:
    include:
      - /var/lib/docker/containers/*/*.log
    include_file_path: true
    start_at: beginning
    operators:
      # Docker wrap log trong JSON: {"log": "...", "stream": "stdout", "time": "..."}
      - type: json_parser
        id: docker_parser
        parse_from: body

      # Parse inner JSON (log line của service)
      - type: json_parser
        id: app_parser
        parse_from: attributes.log
        if: 'attributes["log"] != nil'

      # Lấy timestamp từ field @timestamp của log JSON
      - type: move
        from: attributes["@timestamp"]
        to: attributes.timestamp
        if: 'attributes["@timestamp"] != nil'

      # Lấy service name từ field "service" trong log JSON
      - type: move
        from: attributes.service
        to: resource["service.name"]
        if: 'attributes.service != nil'

      # Bỏ field log thô (đã parse rồi)
      - type: remove
        field: attributes.log

processors:
  # ── Thêm metadata môi trường ──────────────────────────────────────────
  resource:
    attributes:
      - key: deployment.environment
        value: ${ENVIRONMENT}
        action: insert

  # ── Batch để giảm số request tới backend ──────────────────────────────
  batch:
    send_batch_size: 1000
    timeout: 5s

  # ── Lọc bỏ health check và actuator logs (quá nhiều, ít giá trị) ─────
  filter/logs:
    logs:
      exclude:
        match_type: regexp
        record_attributes:
          - key: "http.path"
            value: "^/actuator/.*"

  # ── Sampling: chỉ giữ 100% trace ở dev, 10% ở production ─────────────
  # (sampling tốt hơn nên làm ở tầng SDK trong service, không phải Collector)
  # Để đây để biết option này tồn tại

exporters:
  # ── Traces → Jaeger (qua OTLP) ────────────────────────────────────────
  otlp/jaeger:
    endpoint: jaeger:14317
    tls:
      insecure: true

  # ── Logs → Elasticsearch ──────────────────────────────────────────────
  elasticsearch:
    endpoints: [http://elasticsearch:9200]
    logs_index: hotelhub-logs-${resource["service.name"]}
    sending_queue:
      enabled: true
      num_consumers: 10
      queue_size: 1000
    retry_on_failure:
      enabled: true
      initial_interval: 5s
      max_interval: 30s

  # ── Metrics của Collector tự nó → Prometheus ─────────────────────────
  prometheus:
    endpoint: 0.0.0.0:8888

service:
  pipelines:
    # Traces: nhận từ service → forward sang Jaeger
    traces:
      receivers: [otlp]
      processors: [resource, batch]
      exporters: [otlp/jaeger]

    # Logs: đọc stdout container → lọc → lưu Elasticsearch
    logs:
      receivers: [filelog, otlp]
      processors: [resource, filter/logs, batch]
      exporters: [elasticsearch]

    # Collector internal metrics
    metrics:
      receivers: [otlp]
      processors: [resource, batch]
      exporters: [prometheus]
```

### 11.13. Luồng dữ liệu hoàn chỉnh sau tích hợp

```
[HTTP Request POST /api/place-booking]
        │
        ▼
[API Gateway]
  OTel tạo Span: "HTTP POST /api/place-booking" (traceId: abc123, spanId: s1)
  Inject header: traceparent: 00-abc123-s1-01
        │
        ▼
[Place Booking Service]
  OTel extract context từ header → tiếp tục trace abc123
  OTel tạo Span con: spanId s2, parentSpanId s1
  @Loggable("SAGA_STARTED") → LoggingAspect tạo span s2a, log JSON kèm traceId: abc123
  Log: {"event":"SAGA_STARTED","traceId":"abc123","spanId":"s2a",...}
        │
        ├──[OpenFeign → Booking Service]──────────────────────────────────►
        │   OTel inject header: traceparent: 00-abc123-s3-01
        │   Booking Service: tạo span s3, parentSpanId s2
        │   @Loggable("BOOKING_CREATED") → span s3a, log traceId: abc123
        │
        ├──[OpenFeign → Hotel Service]────────────────────────────────────►
        │   span s4, parentSpanId s2
        │   @Loggable("ROOM_RESERVED") → span s4a, log traceId: abc123
        │
        └──[OpenFeign → Payment Service]──────────────────────────────────►
            span s5, parentSpanId s2
            @Loggable("PAYMENT_INITIATED") → span s5a, log traceId: abc123
            [HTTP call đến VNPay → span s6, parentSpanId s5]
                │
                ▼ Webhook callback
            [Payment Service nhận kết quả]
            @Loggable("PAYMENT_SUCCESS") → span s7, log traceId: abc123
                │
                ▼ Kafka event: payment.completed (header: X-Trace-Id: abc123)
            [Booking Service Kafka consumer]
            KafkaLoggingUtils.setupFromRecord() → restore traceId: abc123
            @Loggable("BOOKING_CONFIRMED") → span s8, log traceId: abc123

Tất cả log ghi ra stdout với "traceId":"abc123"
        │
        ▼
[OTel Collector]
  - Đọc stdout → parse JSON → export logs → Elasticsearch
  - Nhận traces qua OTLP → export → Jaeger

Kết quả:
  Kibana:  search traceId:"abc123" → toàn bộ log của request
  Jaeger:  search traceId abc123  → flame graph toàn bộ trace
  Liên kết: Kibana → copy traceId → Jaeger, hoặc Jaeger → xem log context
```

### 11.14. Hướng dẫn sử dụng Jaeger UI

Sau khi hệ thống chạy, truy cập `http://localhost:16686`:

**Tìm trace theo traceId từ log:**
1. Kibana → tìm log lỗi → copy `traceId`
2. Jaeger UI → tab "Search" → paste vào "Trace ID" → Enter
3. Xem flame graph: span nào chiếm nhiều thời gian nhất

**Tìm trace theo service + operation:**
1. Jaeger UI → tab "Search"
2. Chọn `Service` = `booking-service`
3. Chọn `Operation` = `BOOKING_CREATED` (tên từ `@Loggable(event = ...)`)
4. Set `Min Duration` = `1000ms` để tìm các trace chậm
5. Click "Find Traces"

**Đọc Flame Graph:**
```
Màu xanh = normal span
Màu đỏ  = span có lỗi (exception được record)
Chiều rộng = thời gian tương đối so với tổng trace
```

### 11.15. Tổng hợp thay đổi so với kiến trúc cũ (ELK đơn thuần)

| Thành phần | Trước (ELK) | Sau (ELK + OTel + Jaeger) | Code cần sửa? |
| --- | --- | --- | --- |
| Filebeat | Thu thập log từ container | Bỏ — OTel Collector đảm nhiệm | Không |
| Logstash | Parse và route log | Bỏ — OTel Collector đảm nhiệm | Không |
| Elasticsearch | Lưu log | Lưu log + lưu traces (Jaeger) | Không |
| Kibana | Visualize log | Visualize log | Không |
| OTel Collector | Không có | Thu thập log + traces, route đến ES + Jaeger | Không (hạ tầng) |
| Jaeger | Không có | Lưu trữ và visualize traces | Không |
| `logback-spring.xml` | CONSOLE_JSON | Thêm `OTEL_BRIDGE` appender | Chassis |
| `MdcFilter` | Tự sinh traceId | Ưu tiên lấy từ OTel context | Chassis |
| `LoggingAspect` | Chỉ log | Log + tạo OTel span | Chassis |
| `KafkaLoggingUtils` | Đọc header thủ công | Restore OTel context từ header | Chassis |
| Dependency service | Không có OTel | Thêm OTel SDK + bridge | Mỗi service (1 lần) |
| `application.yml` service | Không có OTel config | Thêm `otel.*` config | Mỗi service (1 lần) |

**Tóm lại:** Phần lớn thay đổi nằm ở **chassis module** và **hạ tầng Docker Compose**. Các service chỉ cần thêm dependency OTel và config yaml một lần — sau đó mọi method annotate `@Loggable` tự động có cả log lẫn span trên Jaeger.