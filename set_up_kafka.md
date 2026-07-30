## Quy trình hoàn chỉnh set up Kafka cho Spring Boot Service

### 📋 BƯỚC 1 — Hạ tầng Kafka (Docker / Broker)

Trước khi code, cần có Kafka broker chạy. Trong dự án bạn dùng **Confluent cp-kafka** + **Zookeeper**:

```yaml
# docker-compose.yml
zookeeper:
  image: confluentinc/cp-zookeeper:7.5.0
  environment:
    ZOOKEEPER_CLIENT_PORT: 2181

kafka:
  image: confluentinc/cp-kafka:7.5.0
  ports:
    - "29092:29092"   # Internal (container ↔ container)
    - "9092:9092"     # External (host machine)
  environment:
    KAFKA_BROKER_ID: 1
    KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
    # 2 listener: internal cho container, external cho localhost
    KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:29092,PLAINTEXT_HOST://0.0.0.0:9092
    KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
    KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
    KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
    KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
```

> **Giải thích LISTENER:**
> - `PLAINTEXT://kafka:29092` → các container Docker khác connect bằng hostname `kafka`, port `29092`
> - `PLAINTEXT_HOST://localhost:9092` → app chạy local trên máy host connect qua `localhost:9092`

---

### 📋 BƯỚC 2 — Maven Dependency (`pom.xml`)

```xml
<!-- Kafka core -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>

<!-- Test -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka-test</artifactId>
    <scope>test</scope>
</dependency>
```

Spring Boot auto-configure sẽ tự tạo `KafkaTemplate`, `ConsumerFactory`, `ProducerFactory` nếu đủ config.

---

### 📋 BƯỚC 3 — `application.properties` (Config trung tâm)

Đây là file **quan trọng nhất**. Ví dụ từ [booking-service](file:///d:/hoc%20tap%20ptit/sem2_year4/VNPT_Internship/hotel-booking-system/services/booking-service/src/main/resources/application.properties#L14-L22):

```properties
# ======================== KAFKA ========================

# 1. BOOTSTRAP SERVERS — Địa chỉ broker(s)
spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:kafka:29092}
# Dùng env variable, fallback = kafka:29092 (Docker internal)
# Chạy local trên host → set KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# ======================== CONSUMER ========================

# 2. GROUP ID — định danh consumer group
spring.kafka.consumer.group-id=${KAFKA_CONSUMER_GROUP:booking-service-group}
# Mỗi service PHẢI có group-id riêng biệt!
# Cùng group-id → message được load-balance (chỉ 1 consumer nhận)
# Khác group-id → message được broadcast (tất cả đều nhận)

# 3. AUTO OFFSET RESET — đọc từ đâu khi consumer group mới
spring.kafka.consumer.auto-offset-reset=earliest
# earliest = đọc từ đầu (không bỏ sót message cũ)
# latest   = chỉ đọc message mới (bỏ qua tất cả message cũ)

# 4. KEY DESERIALIZER — giải mã key
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer

# 5. VALUE DESERIALIZER — giải mã value
spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer
# Nếu value là JSON object → dùng JsonDeserializer (nhưng phức tạp hơn)
# StringDeserializer + ObjectMapper.readValue() linh hoạt hơn ✅

# ======================== PRODUCER ========================

# 6. KEY SERIALIZER — mã hóa key
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer

# 7. VALUE SERIALIZER — mã hóa value  
spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer
# Hoặc: org.springframework.kafka.support.serializer.JsonSerializer (tự serialize object → JSON)

# ======================== OUTBOX ========================
outbox.relay.interval-ms=${OUTBOX_RELAY_INTERVAL_MS:100}
```

#### ⚠️ Serializer vs Deserializer — Tại sao có 2 cách?

| Cách | Producer (serialize) | Consumer (deserialize) | Dùng khi |
|------|---------------------|----------------------|----------|
| **String + ObjectMapper** | `StringSerializer` + `objectMapper.writeValueAsString()` | `StringDeserializer` + `objectMapper.readValue()` | **Linh hoạt**, kiểm soát hoàn toàn. Dự án bạn dùng cách này ✅ |
| **JsonSerializer/Deserializer** | `JsonSerializer` | `JsonDeserializer` | Tự động, nhưng cần config `trusted.packages`, dễ gặp lỗi class not found |

Trong dự án bạn, **booking-service** dùng `StringSerializer` cho cả key + value (producer gửi JSON string), còn **place-booking-service** dùng `JsonSerializer` cho value (producer tự serialize object). **Recommend: thống nhất dùng StringSerializer + ObjectMapper** vì dễ debug và không phụ thuộc classpath.

---

### 📋 BƯỚC 4 — Docker-compose ENV cho mỗi Service

```yaml
booking-service:
  environment:
    KAFKA_BOOTSTRAP_SERVERS: ${KAFKA_BOOTSTRAP_SERVERS:-kafka:29092}
    KAFKA_CONSUMER_GROUP: booking-service-group
  depends_on:
    kafka:
      condition: service_healthy  # ← Rất quan trọng! Đợi Kafka ready
```

---

### 📋 BƯỚC 5 — Khai báo Topic (`@Configuration`)

Có **2 cách** khai báo topic:

#### Cách 1: Java Config Bean (dự án bạn đang dùng)

Từ [KafkaConfig.java](file:///d:/hoc%20tap%20ptit/sem2_year4/VNPT_Internship/hotel-booking-system/services/place_booking_service/src/main/java/com/place_booking_service/configuration/KafkaConfig.java):

```java
@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic bookingCommandsTopic() {
        return TopicBuilder.name("booking-commands")
            .partitions(1)        // Số partition
            .replicas(1)          // Số replica (dev = 1, prod ≥ 3)
            .build();
    }

    @Bean
    public NewTopic paymentEventsTopic() {
        return TopicBuilder.name("payment-events")
            .partitions(3)        // Nhiều partition → nhiều consumer xử lý song song
            .replicas(1)
            .config(TopicConfig.RETENTION_MS_CONFIG, "604800000")  // 7 ngày
            .build();
    }
}
```

#### Cách 2: YAML/Properties (Spring Boot 2.7+)

```properties
spring.kafka.admin.auto-create-topics=true  # default = true
```

> **Lưu ý:** `NewTopic` bean chỉ TẠO topic nếu chưa tồn tại. Nếu topic đã có, nó KHÔNG thay đổi partitions/replicas. Muốn thay đổi → dùng `kafka-topics --alter` CLI.

---

### 📋 BƯỚC 6 — Producer (Gửi message)

#### 6a. KafkaTemplate — Bean tự động

Spring Boot tự tạo `KafkaTemplate` bean từ config. Bạn chỉ cần inject:

```java
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void send(String topic, String key, Object message) {
        kafkaTemplate.send(topic, key, message);
    }
}
```

#### 6b. Outbox Pattern (dự án bạn — production-grade)

Thay vì gửi trực tiếp, **lưu vào DB trước** rồi relay sau:

```
Business Logic (trong @Transactional)
    │
    ├── 1. Save entity vào DB         ← Cùng transaction
    └── 2. Save OutboxMessage vào DB  ← Cùng transaction (đảm bảo atomicity)
         │
         └── @Scheduled OutboxRelay mỗi 100ms
              │
              └── 3. Poll PENDING → Send Kafka → Mark PROCESSED
```

Từ [OutboxPublisherService.java](file:///d:/hoc%20tap%20ptit/sem2_year4/VNPT_Internship/hotel-booking-system/services/place_booking_service/src/main/java/com/place_booking_service/service/OutboxPublisherService.java):

```java
// Bước 1: Lưu outbox message (gọi trong @Transactional cùng business logic)
public void saveOutboxMessage(String topic, Object message, String eventType) {
    String payload = objectMapper.writeValueAsString(message);
    OutboxMessage outboxMessage = new OutboxMessage();
    outboxMessage.setId(UUID.randomUUID());
    outboxMessage.setEventType(eventType);
    outboxMessage.setTopic(topic);
    outboxMessage.setPayload(payload);
    outboxMessage.setStatus("PENDING");
    outboxMessage.setRetryCount(0);
    outboxMessageRepository.save(outboxMessage);
}

// Bước 2: Relay — poll DB và gửi Kafka
@Scheduled(fixedDelayString = "${outbox.publisher.delay:100}")
@Transactional
public void publishPendingMessages() {
    List<OutboxMessage> pending = outboxMessageRepository
        .findTop50ByStatusOrderByCreatedAtAsc("PENDING");
    
    for (OutboxMessage msg : pending) {
        try {
            kafkaProducerService.send(msg.getTopic(), bookingId, payloadObj);
            msg.setStatus("PROCESSED");
        } catch (Exception e) {
            msg.setRetryCount(msg.getRetryCount() + 1);
            if (msg.getRetryCount() >= 5) {
                msg.setStatus("FAILED");  // Dead letter
            }
        }
    }
    outboxMessageRepository.saveAll(pending);
}
```

---

### 📋 BƯỚC 7 — Consumer (Nhận message)

#### 7a. Annotation `@EnableKafka`

Có **2 cách** kích hoạt Kafka consumer:

| Cách | Cách dùng | Khi nào |
|------|-----------|---------|
| **Implicit** | Chỉ cần config `spring.kafka.*` trong properties | Spring Boot auto-config tự bật. Đa số dùng cách này ✅ |
| **Explicit** | `@EnableKafka` trên `@Configuration` class | Khi bạn tạo `ConsumerFactory` thủ công (như [audit-service](file:///d:/hoc%20tap%20ptit/sem2_year4/VNPT_Internship/hotel-booking-system/services/audit-service/src/main/java/com/hotelbooking/auditservice/config/KafkaConsumerConfig.java)) |

#### 7b. `@KafkaListener` — Annotation chính

```java
@Component
public class BookingCommandConsumer {

    // Cách 1: Đơn giản — chỉ topic
    @KafkaListener(topics = "booking-commands")
    public void handle(String payloadJson) {
        // Consumer group lấy từ application.properties
    }

    // Cách 2: Chỉ định groupId ngay trong annotation
    @KafkaListener(
        topics = "audit-log-events", 
        groupId = "${spring.kafka.consumer.group-id:audit-service-group}"
    )
    public void consume(String message) { ... }

    // Cách 3: Nhiều topic cùng lúc
    @KafkaListener(topics = {"booking-events", "payment-events"})
    public void handleMultiple(String message) { ... }

    // Cách 4: Với metadata (key, headers, partition, offset)
    @KafkaListener(topics = "booking-events")
    public void handleWithMeta(
        @Payload String message,
        @Header(KafkaHeaders.RECEIVED_KEY) String key,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset
    ) { ... }

    // Cách 5: Batch consumer (nhận list thay vì từng message)
    @KafkaListener(topics = "audit-log-events", containerFactory = "batchFactory")
    public void handleBatch(List<String> messages) { ... }
}
```

#### 7c. Config Consumer thủ công (`ConsumerFactory`)

Từ [KafkaConsumerConfig.java](file:///d:/hoc%20tap%20ptit/sem2_year4/VNPT_Internship/hotel-booking-system/services/audit-service/src/main/java/com/hotelbooking/auditservice/config/KafkaConsumerConfig.java) — khi cần kiểm soát chi tiết:

```java
@EnableKafka     // ← Bắt buộc khi dùng ConsumerFactory thủ công
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:audit-service-group}")
    private String groupId;

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
        factory.setConsumerFactory(consumerFactory());
        // factory.setConcurrency(3);        // 3 thread consumer
        // factory.setBatchListener(true);    // Batch mode
        return factory;
    }
}
```

> **Khi nào cần config thủ công?** Khi cần: nhiều ConsumerFactory cho topic khác nhau, batch consumer, custom error handler, retry logic, hoặc service chỉ consume (không produce).

---

### 📋 BƯỚC 8 — Tổng quan Annotation

| Annotation | Thuộc | Mục đích |
|-----------|-------|---------|
| `@EnableKafka` | `@Configuration` class | Kích hoạt scan `@KafkaListener`. Tự động khi dùng Spring Boot autoconfigure, **bắt buộc** khi config thủ công |
| `@KafkaListener` | Method | Đăng ký method là Kafka consumer cho topic(s) |
| `@Payload` | Parameter | Inject message body |
| `@Header` | Parameter | Inject Kafka header (key, partition, offset, timestamp...) |
| `@SendTo` | Method (cùng `@KafkaListener`) | Auto-forward kết quả sang topic khác (request-reply pattern) |
| `@KafkaHandler` | Method trong `@KafkaListener` class | Multi-type handler — mỗi method handle 1 kiểu payload |

---

### 📋 BƯỚC 9 — Luồng dữ liệu tổng thể

```
┌─────────────────────────────────────────────────────────────────┐
│ PRODUCER SERVICE (vd: booking-service)                         │
│                                                                 │
│  Business Logic (@Transactional)                                │
│    ├── Save Entity (booking_db)                                 │
│    └── Save OutboxEvent (booking_db.outbox_events)              │
│                                                                 │
│  OutboxRelay (@Scheduled mỗi 100ms)                             │
│    ├── Poll: findTop100ByPublishedFalseOrderByCreatedAtAsc()    │
│    ├── kafkaTemplate.send(topic, key, payload).get()            │
│    └── Mark published = true                                    │
└───────────────────────┬─────────────────────────────────────────┘
                        │
                   Kafka Broker
               Topic: "booking-events"
            Partition 0 | Partition 1 | ...
                        │
┌───────────────────────▼─────────────────────────────────────────┐
│ CONSUMER SERVICE (vd: place-booking-service)                    │
│                                                                 │
│  @KafkaListener(topics = "booking-events")                      │
│  void bookingEventsHandler(String payloadJson)                  │
│    ├── objectMapper.readValue(payloadJson, Map.class)           │
│    ├── Switch on eventType                                      │
│    └── Handle business logic + saveOutboxMessage(next-topic)    │
└─────────────────────────────────────────────────────────────────┘
```

---

### 📋 BƯỚC 10 — Cấu hình PRODUCTION quan trọng

#### 🔴 1. Reliability — Đảm bảo không mất message

```properties
# ===== PRODUCER =====
# Đợi TẤT CẢ replica xác nhận trước khi coi là thành công
spring.kafka.producer.acks=all

# Số lần retry khi gửi thất bại
spring.kafka.producer.retries=3

# Đảm bảo thứ tự + exactly-once (Kafka 3.x+)
spring.kafka.producer.properties.enable.idempotence=true

# Giới hạn request chưa được ack → đảm bảo ordering khi retry
spring.kafka.producer.properties.max.in.flight.requests.per.connection=5

# ===== CONSUMER =====
# KHÔNG auto commit offset → commit sau khi xử lý xong
spring.kafka.consumer.enable-auto-commit=false

# Spring Kafka sẽ commit offset sau khi listener method return thành công
spring.kafka.listener.ack-mode=RECORD
# Các ack-mode:
#   RECORD  = commit sau mỗi record    (an toàn nhất, chậm hơn)
#   BATCH   = commit sau mỗi batch      (cân bằng)
#   MANUAL  = bạn tự gọi ack.acknowledge() (kiểm soát nhất)
```

#### 🔴 2. Performance — Throughput cao

```properties
# ===== PRODUCER =====
# Batch messages trước khi gửi (default 16KB)
spring.kafka.producer.batch-size=32768

# Buffer memory cho producer (default 32MB)  
spring.kafka.producer.buffer-memory=67108864

# Đợi tối đa 5ms để gom batch (trade-off: latency vs throughput)
spring.kafka.producer.properties.linger.ms=5

# Nén message (giảm bandwidth, tăng throughput)
spring.kafka.producer.compression-type=lz4
# Lựa chọn: none | gzip (nén mạnh, CPU cao) | snappy (cân bằng) | lz4 (nhanh nhất) | zstd

# ===== CONSUMER =====
# Số record tối đa mỗi lần poll
spring.kafka.consumer.max-poll-records=500

# Thời gian tối đa giữa 2 lần poll (nếu quá → consumer bị coi là dead)
spring.kafka.consumer.properties.max.poll.interval.ms=300000

# Session timeout — heartbeat detection
spring.kafka.consumer.properties.session.timeout.ms=30000
spring.kafka.consumer.properties.heartbeat.interval.ms=10000

# Concurrency — số thread consumer (nên = số partition)
spring.kafka.listener.concurrency=3
```

#### 🔴 3. Error Handling — Xử lý lỗi

```java
@Bean
public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
    var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
    factory.setConsumerFactory(consumerFactory());
    
    // Retry 3 lần, mỗi lần cách 1s
    factory.setCommonErrorHandler(new DefaultErrorHandler(
        new DeadLetterPublishingRecoverer(kafkaTemplate),  // Gửi sang DLT topic
        new FixedBackOff(1000L, 3)                         // interval, maxAttempts
    ));
    
    return factory;
}
```

> Khi consumer xử lý thất bại 3 lần → message tự chuyển sang topic `<original-topic>.DLT` (Dead Letter Topic).

#### 🔴 4. Security — Production bắt buộc

```properties
# SSL/TLS encryption
spring.kafka.properties.security.protocol=SSL
spring.kafka.ssl.trust-store-location=classpath:truststore.jks
spring.kafka.ssl.trust-store-password=changeit
spring.kafka.ssl.key-store-location=classpath:keystore.jks
spring.kafka.ssl.key-store-password=changeit

# SASL Authentication (nếu dùng Confluent Cloud / managed Kafka)
spring.kafka.properties.security.protocol=SASL_SSL
spring.kafka.properties.sasl.mechanism=PLAIN
spring.kafka.properties.sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required \
    username="api-key" \
    password="api-secret";
```

#### 🔴 5. Monitoring & Health

```properties
# Actuator health check cho Kafka
management.health.kafka.enabled=true

# Expose kafka metrics
management.endpoints.web.exposure.include=health,metrics,kafka
```

#### 🔴 6. Topic — Production settings

```java
@Bean
public NewTopic auditLogTopic() {
    return TopicBuilder.name("audit-log-events")
        .partitions(6)             // = số consumer tối đa song song
        .replicas(3)               // Production: ≥ 3 (fault tolerance)
        .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")  // ≥ 2 replica sync
        .config(TopicConfig.RETENTION_MS_CONFIG, "2592000000") // 30 ngày
        .config(TopicConfig.CLEANUP_POLICY_CONFIG, "delete")   // hoặc "compact"
        .build();
}
```

---

### 📋 Checklist tổng hợp: Setup Kafka cho 1 service

| # | Bước | File / Vị trí | Bắt buộc? |
|---|------|--------------|-----------|
| 1 | Kafka broker trong Docker | `docker-compose.yml` | ✅ |
| 2 | `spring-kafka` dependency | `pom.xml` | ✅ |
| 3 | `bootstrap-servers` | `application.properties` | ✅ |
| 4 | `group-id` (nếu consume) | `application.properties` | ✅ consumer |
| 5 | Serializer (producer) | `application.properties` | ✅ producer |
| 6 | Deserializer (consumer) | `application.properties` | ✅ consumer |
| 7 | `auto-offset-reset` | `application.properties` | ✅ consumer |
| 8 | Topic declaration | `KafkaConfig.java` (@Bean NewTopic) | 🔶 nên có |
| 9 | Producer service | `KafkaTemplate` inject | ✅ producer |
| 10 | Consumer listener | `@KafkaListener` method | ✅ consumer |
| 11 | ENV in docker-compose | `KAFKA_BOOTSTRAP_SERVERS`, `depends_on: kafka` | ✅ |
| 12 | `acks=all`, `enable-auto-commit=false` | `application.properties` | 🔴 production |
| 13 | Error handler / DLT | `KafkaConsumerConfig.java` | 🔴 production |
| 14 | SSL / SASL | `application.properties` | 🔴 production |

---

### ⚡ So sánh 2 kiểu setup trong dự án bạn

| | **booking-service** (Producer + Consumer) | **audit-service** (Consumer only) |
|---|---|---|
| Config | Chỉ dùng `application.properties` | `application.properties` + `KafkaConsumerConfig.java` (thủ công) |
| `@EnableKafka` | Không cần (auto-config) | Cần (vì tạo ConsumerFactory thủ công) |
| Producer | `KafkaTemplate<String, String>` | Không có |
| Consumer | `@KafkaListener` | `@KafkaListener` |
| Topic | Không khai báo (tạo bởi place-booking-service) | Không khai báo |
| Outbox | `OutboxRelay` + `@Scheduled` | Không có |
