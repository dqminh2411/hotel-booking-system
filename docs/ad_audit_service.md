# PHÂN TÍCH & THIẾT KẾ AUDIT SERVICE — QUẢN LÝ LOG NGHIỆP VỤ

## HotelHub SaaS Platform

- **Phiên bản:** 1.0 (Draft)
- **Ngày:** 29/07/2026
- **Trạng thái:** Phân tích & Thiết kế (chưa cài đặt)

---

## MỤC LỤC

- [1. Tổng quan](#1-tổng-quan)
- [2. Kiến trúc tổng quan](#2-kiến-trúc-tổng-quan)
- [3. Luồng ghi log chi tiết](#3-luồng-ghi-log-chi-tiết)
- [4. Thiết kế Outbox Audit Event tại các Source Service](#4-thiết-kế-outbox-audit-event-tại-các-source-service)
- [5. Kafka Topic & Consumer](#5-kafka-topic--consumer)
- [6. MongoDB Audit Store — Schema & Indexes](#6-mongodb-audit-store--schema--indexes)
- [7. Danh sách sự kiện nghiệp vụ cần ghi log](#7-danh-sách-sự-kiện-nghiệp-vụ-cần-ghi-log)
- [8. Audit Service — REST API](#8-audit-service--rest-api)
- [9. Idempotency](#9-idempotency)
- [10. Retention Policy](#10-retention-policy)
- [11. Nguyên tắc thiết kế (Production Best Practices)](#11-nguyên-tắc-thiết-kế-production-best-practices)
- [12. Kế hoạch triển khai](#12-kế-hoạch-triển-khai)

---

# 1. Tổng quan

## 1.1. Mục tiêu

Xây dựng **Audit Service** — một microservice chuyên biệt chịu trách nhiệm:

1. **Thu nhận** (consume) các sự kiện nghiệp vụ (audit event) từ tất cả các service qua Kafka
2. **Lưu trữ** audit event vào MongoDB (append-only, immutable)
3. **Cung cấp API** (REST) cho Admin/Owner truy vấn, tìm kiếm audit log
4. **Đảm bảo** dữ liệu log nhất quán, không mất, không trùng lặp (idempotent)

## 1.2. Công nghệ

| Component | Công nghệ | Lý do |
|-----------|-----------|-------|
| Audit Service | Spring Boot 3 + Java 21 | Đồng bộ tech stack |
| Audit Store | **MongoDB** | Schema linh hoạt, write append-only performance tốt, học NoSQL |
| Message Broker | Apache Kafka (đã có) | Tận dụng hạ tầng hiện có |
| Service Discovery | Eureka (đã có) | Đồng bộ |
| Outbox Table | PostgreSQL (tại DB của từng source service) | Outbox pattern yêu cầu cùng transaction với business data |

## 1.3. Phạm vi Audit Log

- **Ghi log:** Tất cả sự kiện nghiệp vụ quan trọng (tạo/sửa/xóa/chuyển trạng thái các entity chính)
- **KHÔNG ghi log:** Hành động tìm kiếm khách sạn (volume quá lớn, ít giá trị audit)

---

# 2. Kiến trúc tổng quan

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                         AUDIT LOG ARCHITECTURE                                   │
│                                                                                  │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌──────────────┐   │
│  │   User     │ │   Hotel    │ │  Booking   │ │  Payment   │ │  Promotion   │   │
│  │  Service   │ │  Service   │ │  Service   │ │  Service   │ │   Service    │   │
│  │            │ │            │ │            │ │            │ │              │   │
│  │ ┌────────┐ │ │ ┌────────┐ │ │ ┌────────┐ │ │ ┌────────┐ │ │ ┌──────────┐ │   │
│  │ │ audit_ │ │ │ │ audit_ │ │ │ │ outbox │ │ │ │ outbox │ │ │ │ outbox   │ │   │
│  │ │ outbox │ │ │ │ outbox │ │ │ │ _events│ │ │ │ _msg   │ │ │ │ _events  │ │   │
│  │ └───┬────┘ │ │ └───┬────┘ │ │ └───┬────┘ │ │ └───┬────┘ │ │ └────┬─────┘ │   │
│  └─────┼──────┘ └─────┼──────┘ └─────┼──────┘ └─────┼──────┘ └──────┼───────┘   │
│        │              │              │              │               │            │
│        │     Outbox Relay (polling, per-service)     │               │            │
│        ▼              ▼              ▼              ▼               ▼            │
│  ┌──────────────────────────────────────────────────────────────────────────┐    │
│  │                  Kafka Topic: "audit-log-events"                         │    │
│  └─────────────────────────────┬────────────────────────────────────────────┘    │
│                                │                                                 │
│                                ▼                                                 │
│                  ┌──────────────────────────┐                                    │
│                  │      AUDIT SERVICE        │                                    │
│                  │                           │                                    │
│                  │  ┌─────────────────────┐  │                                    │
│                  │  │ AuditLogConsumer     │  │                                    │
│                  │  │ (KafkaListener)      │  │                                    │
│                  │  │                      │  │                                    │
│                  │  │ • Deserialize event  │  │                                    │
│                  │  │ • Idempotency check  │  │                                    │
│                  │  │ • Write to MongoDB   │  │                                    │
│                  │  └──────────┬───────────┘  │                                    │
│                  │             │              │                                    │
│                  │             ▼              │                                    │
│                  │  ┌─────────────────────┐  │                                    │
│                  │  │    MongoDB           │  │                                    │
│                  │  │    (audit_db)         │  │                                    │
│                  │  │                      │  │                                    │
│                  │  │  Collection:          │  │                                    │
│                  │  │  audit_logs           │  │                                    │
│                  │  └──────────┬───────────┘  │                                    │
│                  │             │              │                                    │
│                  │  ┌─────────────────────┐  │                                    │
│                  │  │ AuditLogController   │  │                                    │
│                  │  │ (REST API)           │  │                                    │
│                  │  │                      │  │                                    │
│                  │  │ GET /api/audit-logs  │  │                                    │
│                  │  │ GET /api/audit-logs/ │  │                                    │
│                  │  │   entity/{type}/{id} │  │                                    │
│                  │  │ GET /api/audit-logs/ │  │                                    │
│                  │  │   statistics         │  │                                    │
│                  │  └─────────────────────┘  │                                    │
│                  └──────────────────────────┘                                    │
│                                                                                  │
│                               ▲                                                  │
│                               │ REST API                                         │
│                  ┌────────────┴───────────────┐                                  │
│                  │  Admin Console /             │                                  │
│                  │  Owner Dashboard (Frontend)  │                                  │
│                  └──────────────────────────────┘                                  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

# 3. Luồng ghi log chi tiết

## 3.1. Sequence: Từ business action đến audit store

```
Actor (User/Staff/Admin)
    │
    │  HTTP Request (JWT chứa userId, tenantId, role)
    ▼
API Gateway (route + JWT decode)
    │
    ▼
Source Service (vd: Booking Service)
    │
    │  @Transactional (cùng 1 DB transaction)
    │
    ├──① Thực hiện business logic
    │     (INSERT/UPDATE entity chính — vd: booking)
    │
    ├──② Tạo AuditEvent object (payload):
    │     {
    │       "eventId": "uuid-unique",
    │       "timestamp": "2026-07-29T10:30:00Z",
    │       "tenantId": "tenant-123",
    │       "actorId": "user-456",
    │       "actorType": "CUSTOMER",
    │       "actorEmail": "customer@example.com",
    │       "actorIp": "192.168.1.100",
    │       "action": "BOOKING_CREATED",
    │       "actionCategory": "BOOKING",
    │       "severity": "INFO",
    │       "entityType": "BOOKING",
    │       "entityId": "booking-789",
    │       "serviceName": "booking-service",
    │       "correlationId": "corr-abc-123",
    │       "description": "Khách hàng tạo đơn đặt phòng mới",
    │       "oldValue": null,
    │       "newValue": { "status": "PENDING", "totalAmount": 1500000 },
    │       "metadata": { "roomTypeId": "rt-001", "checkIn": "2026-08-01" },
    │       "resultStatus": "SUCCESS",
    │       "errorMessage": null
    │     }
    │
    ├──③ Lưu AuditEvent (serialized) vào bảng outbox_events
    │     CÙNG transaction với bước ①
    │     (payload = JSON serialized AuditEvent, topic = "audit-log-events")
    │
    └──④ COMMIT transaction
         (đảm bảo: business data + outbox event đồng nhất)

    ═══ (Ranh giới transaction - async từ đây) ═══

Outbox Relay (scheduled, per-service)
    │
    ├──⑤ Poll bảng outbox_events WHERE published = false
    │     (đọc top N sự kiện chưa publish, sắp xếp theo created_at)
    │
    ├──⑥ Gửi payload lên Kafka topic "audit-log-events"
    │     key = entityId (đảm bảo ordering per entity)
    │
    └──⑦ Đánh dấu outbox event là published = true

Kafka Topic: "audit-log-events"
    │
    ▼
Audit Service — AuditLogConsumer (@KafkaListener)
    │
    ├──⑧ Deserialize message thành AuditEvent
    │
    ├──⑨ Kiểm tra idempotency:
    │     Truy vấn MongoDB theo eventId
    │     Nếu đã tồn tại → skip (không insert lại)
    │
    ├──⑩ Nếu chưa tồn tại → INSERT vào MongoDB collection "audit_logs"
    │
    └──⑪ Commit Kafka offset
```

## 3.2. Tại sao lưu AuditEvent vào outbox (thay vì publish thẳng Kafka)?

```
❌ Direct publish: 
   Business logic commit DB ✅ → Kafka publish fail ❌ → MẤT audit log

✅ Outbox pattern:
   Business logic + outbox event commit CÙNG transaction ✅
   → Outbox Relay publish sau ✅ → Nếu Kafka down, retry ✅
   → KHÔNG BAO GIỜ mất audit log
```

## 3.3. Tái sử dụng bảng outbox hiện tại

Các service đã có bảng outbox (booking-service, payment-service, place-booking-service, promotion-service) có thể **tái sử dụng chung** bảng `outbox_events` hiện có. Audit event chỉ khác ở giá trị `topic`:

| Event loại | Topic |
|-----------|-------|
| Business command/event (hiện tại) | `booking-events`, `payment-events`, `payment-commands`, ... |
| Audit event (mới) | `audit-log-events` |

Outbox Relay **hiện tại** đã tự đọc `topic` từ mỗi outbox row → **không cần sửa Outbox Relay**, chỉ cần ghi outbox event mới với `topic = "audit-log-events"`.

Đối với các service **chưa có outbox** (vd: user-service, hotel-service): cần thêm bảng `outbox_events` và Outbox Relay tương tự.

---

# 4. Thiết kế Outbox Audit Event tại các Source Service

## 4.1. AuditEvent DTO (payload sẽ lưu vào outbox)

```java
/**
 * DTO chứa thông tin audit event — được serialized thành JSON
 * và lưu vào cột `payload` của bảng outbox_events.
 * 
 * Đây là contract giữa source service (producer) và audit service (consumer).
 */
public class AuditEvent {
    // === Identification ===
    private String eventId;           // UUID, unique per event — dùng cho idempotency
    private Instant timestamp;        // Thời điểm sự kiện xảy ra

    // === Tenant (multi-tenant isolation) ===
    private String tenantId;          // UUID, ID của tenant

    // === Actor (Ai thực hiện?) ===
    private String actorId;           // UUID, null nếu là SYSTEM
    private String actorType;         // CUSTOMER | STAFF | OWNER | ADMIN | SYSTEM
    private String actorEmail;        // Snapshot email tại thời điểm log
    private String actorIp;           // IPv4 hoặc IPv6

    // === Action (Hành động gì?) ===
    private String action;            // VD: BOOKING_CREATED, PAYMENT_SUCCESS
    private String actionCategory;    // AUTH | BOOKING | PAYMENT | HOTEL | PROMOTION | ADMIN | NOTIFICATION
    private String severity;          // INFO | WARN | ERROR

    // === Entity (Trên đối tượng nào?) ===
    private String entityType;        // BOOKING | HOTEL | USER | PAYMENT | ROOM | PROMOTION
    private String entityId;          // ID của đối tượng bị tác động

    // === Context ===
    private String serviceName;       // booking-service, payment-service, ...
    private String correlationId;     // Trace/correlation ID xuyên service (nullable)
    private String description;       // Mô tả ngắn bằng ngôn ngữ tự nhiên

    // === Data change tracking ===
    private Object oldValue;          // Giá trị trước (Map/POJO, nullable)
    private Object newValue;          // Giá trị sau (Map/POJO, nullable)
    private Object metadata;          // Dữ liệu bổ sung (nullable)

    // === Result ===
    private String resultStatus;      // SUCCESS | FAILURE | PARTIAL
    private String errorMessage;      // Lý do thất bại (nullable)
}
```

## 4.2. Cách ghi audit event trong business logic

```java
// === Ví dụ: Booking Service — handleConfirmBooking ===

@Transactional
public void handleConfirmBooking(UUID sagaId, UUID bookingId) {
    BookingEntity booking = bookingRepository.findByBookingId(bookingId)
        .orElseThrow(() -> new AppException(...));

    BookingStatus oldStatus = booking.getStatus();
    booking.setStatus(BookingStatus.CONFIRMED);
    bookingRepository.save(booking);

    // --- Ghi business outbox event (hiện tại, giữ nguyên) ---
    saveOutboxEvent(new BookingConfirmed(sagaId, "BookingConfirmed", bookingDetail));

    // --- Ghi AUDIT outbox event (MỚI) ---
    AuditEvent auditEvent = AuditEvent.builder()
        .eventId(UUID.randomUUID().toString())
        .timestamp(Instant.now())
        .tenantId(booking.getTenantId())
        .actorId(null)                          // SYSTEM action (Saga tự động)
        .actorType("SYSTEM")
        .action("BOOKING_CONFIRMED")
        .actionCategory("BOOKING")
        .severity("INFO")
        .entityType("BOOKING")
        .entityId(bookingId.toString())
        .serviceName("booking-service")
        .correlationId(sagaId.toString())
        .description("Booking xác nhận tự động sau thanh toán thành công")
        .oldValue(Map.of("status", oldStatus.name()))
        .newValue(Map.of("status", "CONFIRMED"))
        .resultStatus("SUCCESS")
        .build();

    saveAuditOutboxEvent(auditEvent);           // Lưu vào outbox, topic = "audit-log-events"
}

// --- Helper method ---
private void saveAuditOutboxEvent(AuditEvent auditEvent) {
    OutboxEventEntity outbox = new OutboxEventEntity();
    outbox.setId(UUID.randomUUID());
    outbox.setTopic("audit-log-events");        // Topic riêng cho audit
    outbox.setPayload(objectMapper.writeValueAsString(auditEvent));
    outbox.setPublished(false);
    outbox.setCreatedAt(Instant.now());
    outboxEventRepository.save(outbox);
}
```

**Lưu ý:** Cả business outbox event (booking-events) và audit outbox event (audit-log-events) đều nằm **cùng bảng `outbox_events`**, chỉ khác giá trị `topic`. Outbox Relay hiện có sẽ tự xử lý cả hai.

## 4.3. Bảng outbox_events (tái sử dụng schema hiện tại)

Schema giữ nguyên như booking-service đang dùng:

```sql
CREATE TABLE IF NOT EXISTS outbox_events (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    topic         VARCHAR(100) NOT NULL,       -- "audit-log-events" cho audit
    payload       JSONB        NOT NULL,       -- AuditEvent serialized
    published     BOOLEAN      NOT NULL DEFAULT false,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    published_at  TIMESTAMP,
    is_deleted    BOOLEAN      NOT NULL DEFAULT false
);

-- Index cho Outbox Relay (đã có)
CREATE INDEX IF NOT EXISTS idx_outbox_unpublished
    ON outbox_events (published, created_at)
    WHERE published = false;
```

Các service chưa có bảng outbox (user-service, hotel-service) cần tạo bảng này + thêm Outbox Relay component.

---

# 5. Kafka Topic & Consumer

## 5.1. Topic mới

| Topic | Partitions | Replication | Retention | Mô tả |
|-------|-----------|-------------|-----------|-------|
| `audit-log-events` | 3 | 1 (dev) / 3 (prod) | 7 ngày | Chuyển audit event từ source service → audit service |

**Partition key:** `entityId` — đảm bảo tất cả log của cùng 1 entity rơi vào cùng partition → giữ ordering.

## 5.2. Consumer Group

| Consumer Group | Service | Mô tả |
|---------------|---------|-------|
| `audit-service-group` | Audit Service | Duy nhất 1 consumer group |

## 5.3. Consumer Error Handling

```
audit-log-events (main topic)
        │
        ▼
AuditLogConsumer
        │
    ┌───┴───┐
    │       │
  ✅ OK   ❌ Deserialize/Validation fail
    │       │
    ▼       ▼
  Insert   audit-log-events-dlq (Dead Letter Queue)
  MongoDB  → Manual review sau
```

- **Deserialization error:** Gửi vào DLQ `audit-log-events-dlq`
- **MongoDB down:** Consumer dừng consume (Kafka sẽ retry khi consumer restart) — audit event không mất vì nằm trong Kafka retention 7 ngày
- **Duplicate event:** Skip (idempotency check)

---

# 6. MongoDB Audit Store — Schema & Indexes

## 6.1. Database & Collection

| Database | Collection | Mô tả |
|----------|-----------|-------|
| `audit_db` | `audit_logs` | Collection chính, append-only |

## 6.2. Document Schema

```json
{
  "_id": ObjectId("..."),                    // MongoDB auto-gen

  // === Identification ===
  "eventId": "550e8400-e29b-41d4-a716-446655440000",   // UUID, unique — idempotency key
  "timestamp": ISODate("2026-07-29T10:30:00Z"),

  // === Tenant ===
  "tenantId": "tenant-uuid-123",

  // === Actor ===
  "actor": {
    "id": "user-uuid-456",              // null nếu SYSTEM
    "type": "CUSTOMER",                 // CUSTOMER | STAFF | OWNER | ADMIN | SYSTEM
    "email": "customer@example.com",    // snapshot tại thời điểm log
    "ip": "192.168.1.100"
  },

  // === Action ===
  "action": "BOOKING_CREATED",
  "actionCategory": "BOOKING",          // AUTH | BOOKING | PAYMENT | HOTEL | PROMOTION | ADMIN | NOTIFICATION
  "severity": "INFO",                   // INFO | WARN | ERROR

  // === Entity ===
  "entity": {
    "type": "BOOKING",
    "id": "booking-uuid-789"
  },

  // === Context ===
  "serviceName": "booking-service",
  "correlationId": "saga-uuid-abc",     // nullable
  "description": "Khách hàng tạo đơn đặt phòng mới tại Khách sạn ABC",

  // === Data Change ===
  "oldValue": null,                     // nullable, flexible BSON document
  "newValue": {                         // nullable, flexible BSON document
    "status": "PENDING",
    "roomTypeId": "rt-001",
    "checkIn": "2026-08-01",
    "checkOut": "2026-08-03",
    "totalAmount": 1500000
  },
  "metadata": {                         // nullable, bất kỳ dữ liệu bổ sung nào
    "idempotencyKey": "idem-xyz",
    "sagaId": "saga-uuid-abc"
  },

  // === Result ===
  "resultStatus": "SUCCESS",            // SUCCESS | FAILURE | PARTIAL
  "errorMessage": null,                 // nullable

  // === Managed by Audit Service ===
  "receivedAt": ISODate("2026-07-29T10:30:01Z"),  // Thời điểm Audit Service nhận được
  "expiresAt": ISODate("2027-07-29T10:30:00Z")    // TTL = timestamp + 12 tháng (retention)
}
```

## 6.3. Indexes

```javascript
// === Index 1: Idempotency — đảm bảo mỗi eventId chỉ insert 1 lần ===
db.audit_logs.createIndex(
  { "eventId": 1 },
  { unique: true, name: "idx_eventId_unique" }
);

// === Index 2: Truy vấn chính — log theo tenant, sắp xếp thời gian ===
db.audit_logs.createIndex(
  { "tenantId": 1, "timestamp": -1 },
  { name: "idx_tenant_timestamp" }
);

// === Index 3: Truy vấn theo actor — "ai đã làm gì?" ===
db.audit_logs.createIndex(
  { "actor.id": 1, "timestamp": -1 },
  { name: "idx_actor_timestamp" }
);

// === Index 4: Truy vấn theo entity — "lịch sử thay đổi entity X" ===
db.audit_logs.createIndex(
  { "entity.type": 1, "entity.id": 1, "timestamp": -1 },
  { name: "idx_entity_timestamp" }
);

// === Index 5: Truy vấn theo action category ===
db.audit_logs.createIndex(
  { "actionCategory": 1, "timestamp": -1 },
  { name: "idx_actionCategory_timestamp" }
);

// === Index 6: Truy vấn theo correlation ID (tracing xuyên service) ===
db.audit_logs.createIndex(
  { "correlationId": 1 },
  { 
    name: "idx_correlationId",
    sparse: true                   // Chỉ index document có correlationId != null
  }
);

// === Index 7: TTL — tự động xóa document sau 12 tháng ===
db.audit_logs.createIndex(
  { "expiresAt": 1 },
  { 
    name: "idx_ttl_expiry",
    expireAfterSeconds: 0          // Xóa khi current time >= expiresAt
  }
);

// === Index 8: Full-text search trên description ===
db.audit_logs.createIndex(
  { "description": "text" },
  { name: "idx_description_text" }
);
```

> **Lưu ý về TTL Index:** MongoDB tự động xóa document khi `expiresAt` đã qua. Audit Service set `expiresAt = timestamp + 12 tháng` khi insert. Đây là cách đơn giản nhất để quản lý retention.

---

# 7. Danh sách sự kiện nghiệp vụ cần ghi log

## 7.1. User Service

| Action Code | Mô tả | Severity | Capture old/new? |
|-------------|-------|----------|-------------------|
| `USER_REGISTERED` | Đăng ký tài khoản mới | INFO | new_value: userId |
| `USER_LOGIN_SUCCESS` | Đăng nhập thành công | INFO | Không |
| `USER_LOGIN_FAILED` | Đăng nhập thất bại | WARN | metadata: email attempt |
| `USER_PROFILE_UPDATED` | Cập nhật hồ sơ cá nhân | INFO | Có (trường thay đổi) |
| `USER_ACCOUNT_LOCKED` | Admin khóa tài khoản | WARN | Có (lý do khóa) |
| `USER_ACCOUNT_UNLOCKED` | Admin mở khóa tài khoản | INFO | Có (lý do mở) |
| `USER_PASSWORD_CHANGED` | Đổi mật khẩu | INFO | Không (KHÔNG log password) |

## 7.2. Hotel Service

| Action Code | Mô tả | Severity | Capture old/new? |
|-------------|-------|----------|-------------------|
| `HOTEL_CREATED` | Tạo khách sạn mới | INFO | new_value: hotel info |
| `HOTEL_UPDATED` | Cập nhật thông tin KS | INFO | Có |
| `HOTEL_DELETED` | Xóa khách sạn | WARN | old_value: snapshot |
| `ROOM_TYPE_CREATED` | Tạo loại phòng | INFO | new_value |
| `ROOM_TYPE_UPDATED` | Cập nhật loại phòng | INFO | Có |
| `ROOM_CREATED` | Thêm phòng | INFO | new_value |
| `ROOM_DELETED` | Xóa phòng | INFO | old_value |
| `ROOM_STATUS_CHANGED` | Cập nhật trạng thái phòng | INFO | Có (old_status → new_status) |
| `PRICING_RULE_CREATED` | Tạo rule giá động | INFO | new_value |
| `PRICING_RULE_UPDATED` | Sửa rule giá động | INFO | Có |
| `PRICING_RULE_DELETED` | Xóa rule giá động | INFO | old_value |

## 7.3. Booking Service

| Action Code | Mô tả | Severity | Capture old/new? |
|-------------|-------|----------|-------------------|
| `BOOKING_CREATED` | Tạo booking (Pending) | INFO | new_value: booking info |
| `BOOKING_CONFIRMED` | Booking xác nhận tự động | INFO | Có (PENDING → CONFIRMED) |
| `BOOKING_CANCELLED_BY_CUSTOMER` | Khách hàng hủy booking | INFO | Có + lý do |
| `BOOKING_CANCELLED_BY_STAFF` | Nhân viên hủy booking | WARN | Có + staff_id + lý do |
| `BOOKING_CANCELLED_BY_SYSTEM` | Hệ thống hủy tự động | WARN | Có + lý do |
| `BOOKING_CHECKED_IN` | Check-in | INFO | Có (CONFIRMED → CHECKEDIN) |
| `BOOKING_CHECKED_OUT` | Check-out | INFO | Có (CHECKEDIN → COMPLETED) |

## 7.4. Payment Service

| Action Code | Mô tả | Severity | Capture old/new? |
|-------------|-------|----------|-------------------|
| `PAYMENT_INITIATED` | Tạo giao dịch thanh toán | INFO | new_value: amount, method |
| `PAYMENT_SUCCESS` | Thanh toán thành công | INFO | Có (transaction_id) |
| `PAYMENT_FAILED` | Thanh toán thất bại | WARN | Có (error_code, reason) |
| `REFUND_INITIATED` | Khởi tạo hoàn tiền | INFO | Có (amount, booking_id) |
| `REFUND_SUCCESS` | Hoàn tiền thành công | INFO | Có |
| `REFUND_FAILED` | Hoàn tiền thất bại | ERROR | Có (reason) |

## 7.5. Promotion Service

| Action Code | Mô tả | Severity | Capture old/new? |
|-------------|-------|----------|-------------------|
| `PROMOTION_CREATED` | Tạo chương trình khuyến mãi | INFO | new_value |
| `PROMOTION_UPDATED` | Cập nhật khuyến mãi | INFO | Có |
| `PROMOTION_DEACTIVATED` | Vô hiệu hóa khuyến mãi | WARN | Có |
| `COUPON_APPLIED` | Áp dụng coupon vào booking | INFO | Có (code, discount) |
| `COUPON_REJECTED` | Coupon bị từ chối | INFO | metadata: reason |

## 7.6. Admin Actions

| Action Code | Mô tả | Severity | Capture old/new? |
|-------------|-------|----------|-------------------|
| `HOTEL_APPROVED` | Duyệt khách sạn | INFO | Có (PENDING → APPROVED) |
| `HOTEL_REJECTED` | Từ chối khách sạn | WARN | Có + lý do |
| `HOTEL_SUSPENDED` | Tạm ngưng khách sạn | WARN | Có + lý do |
| `TENANT_SUBSCRIPTION_CHANGED` | Thay đổi gói thuê bao | INFO | Có (old_plan → new_plan) |

## 7.7. Notification Service

| Action Code | Mô tả | Severity | Capture old/new? |
|-------------|-------|----------|-------------------|
| `NOTIFICATION_SENT` | Gửi thông báo thành công | INFO | metadata: channel, recipient |
| `NOTIFICATION_FAILED` | Gửi thông báo thất bại | WARN | metadata: error, retry_count |
| `PROMOTION_BROADCAST_SENT` | Gửi broadcast khuyến mãi | INFO | metadata: target_count |

---

# 8. Audit Service — REST API

Chi tiết API spec đầy đủ xem tại: [audit-service.yaml](./api-specs/audit-service.yaml)

## 8.1. Tổng quan Endpoints

| Method | Endpoint | Mô tả | Quyền truy cập |
|--------|----------|-------|----------------|
| `GET` | `/api/audit-logs` | Truy vấn log theo nhiều filter | ADMIN, OWNER |
| `GET` | `/api/audit-logs/{eventId}` | Xem chi tiết 1 audit log entry | ADMIN, OWNER |
| `GET` | `/api/audit-logs/entity/{entityType}/{entityId}/history` | Lịch sử thay đổi entity | ADMIN, OWNER, STAFF |
| `GET` | `/api/audit-logs/statistics` | Thống kê audit log | ADMIN |

## 8.2. Phân quyền

| Vai trò | Phạm vi xem log |
|---------|-----------------|
| **Platform Admin** | Xem toàn bộ log mọi tenant |
| **Hotel Owner** | Xem log trong phạm vi tenant của mình (tự động filter bởi `tenantId` từ JWT) |
| **Hotel Staff** | Xem log booking/check-in/check-out tại khách sạn được gán |
| **Customer** | Không có quyền truy cập Audit API (trong phase này) |

## 8.3. Query Parameters chính (GET /api/audit-logs)

| Parameter | Type | Mô tả |
|-----------|------|-------|
| `startDate` | ISO-8601 | Bắt đầu khoảng thời gian |
| `endDate` | ISO-8601 | Kết thúc khoảng thời gian |
| `actorId` | UUID | Lọc theo actor |
| `actorType` | String | CUSTOMER, STAFF, OWNER, ADMIN, SYSTEM |
| `action` | String | Action code (VD: BOOKING_CREATED) |
| `actionCategory` | String | AUTH, BOOKING, PAYMENT, HOTEL, ... |
| `entityType` | String | BOOKING, HOTEL, USER, PAYMENT, ... |
| `entityId` | String | ID cụ thể entity |
| `severity` | String | INFO, WARN, ERROR |
| `resultStatus` | String | SUCCESS, FAILURE |
| `search` | String | Full-text search trên description |
| `page` | int | Trang (default: 0) |
| `size` | int | Số lượng mỗi trang (default: 20, max: 100) |
| `sort` | String | Sắp xếp (default: timestamp,desc) |

---

# 9. Idempotency

## 9.1. Vấn đề

Kafka delivery guarantee là **at-least-once** → consumer có thể nhận cùng 1 message nhiều lần (do rebalance, crash, retry...). Nếu không xử lý, MongoDB sẽ có duplicate audit log.

## 9.2. Giải pháp

```
AuditLogConsumer nhận message
    │
    ├──① Extract eventId từ AuditEvent payload
    │
    ├──② MongoDB: INSERT với eventId
    │     (eventId có unique index)
    │
    ├──── Nếu insert thành công → ✅ Done
    │
    └──── Nếu DuplicateKeyException → ⚠️ Skip (đã insert trước đó)
          (log.debug, KHÔNG throw exception, commit offset)
```

```java
// Pseudo-code
@KafkaListener(topics = "audit-log-events", groupId = "audit-service-group")
public void consume(String message) {
    AuditEvent event = objectMapper.readValue(message, AuditEvent.class);

    try {
        AuditLogDocument doc = mapToDocument(event);
        doc.setReceivedAt(Instant.now());
        doc.setExpiresAt(event.getTimestamp().plus(365, ChronoUnit.DAYS));  // 12 tháng
        
        auditLogRepository.insert(doc);  // Dùng insert() — KHÔNG dùng save()
                                         // insert() sẽ throw DuplicateKeyException nếu trùng
    } catch (DuplicateKeyException e) {
        log.debug("Duplicate audit event, skipping: eventId={}", event.getEventId());
        // Không throw → Kafka commit offset → không consume lại
    }
}
```

**Tại sao dùng `insert()` thay vì kiểm tra `findByEventId()` trước?**
- `insert()` + unique index = **atomic** idempotency check ở DB level
- `find → insert` = 2 operations, có race condition khi 2 consumer instance cùng check

---

# 10. Retention Policy

## 10.1. Chiến lược: 12 tháng online

```
┌─────────────────────────────────────────────────────────┐
│                  RETENTION = 12 THÁNG                    │
│                                                          │
│  Document được tạo                                       │
│      │                                                   │
│      │  expiresAt = timestamp + 365 ngày                 │
│      │                                                   │
│      ▼                                                   │
│  ┌──────────────────────────────────────────────────┐    │
│  │  HOT DATA (0-12 tháng)                            │    │
│  │  MongoDB collection "audit_logs"                  │    │
│  │  Full query, full index, real-time                │    │
│  └──────────────────────────────────────────────────┘    │
│      │                                                   │
│      │  MongoDB TTL Index tự động xóa                    │
│      │  khi current_time >= expiresAt                    │
│      ▼                                                   │
│  ┌──────────────────────────────────────────────────┐    │
│  │  DELETED                                          │    │
│  │  (MongoDB background thread xóa tự động)          │    │
│  └──────────────────────────────────────────────────┘    │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

## 10.2. TTL Index (đã khai báo ở Mục 6.3)

```javascript
db.audit_logs.createIndex(
  { "expiresAt": 1 },
  { expireAfterSeconds: 0 }
);
```

MongoDB background thread chạy mỗi 60 giây, kiểm tra và xóa document có `expiresAt <= now()`.

## 10.3. Tương lai (nếu cần archive)

Nếu sau này cần lưu log lâu hơn (compliance), có thể thêm scheduled job export data trước khi bị TTL xóa → lưu sang file JSON/Parquet → upload MinIO/S3.

---

# 11. Nguyên tắc thiết kế (Production Best Practices)

Áp dụng từ phần 3.4 tài liệu business_log.md:

| # | Nguyên tắc | Cách áp dụng trong thiết kế |
|---|-----------|---------------------------|
| 1 | **Immutable** (Bất biến) | MongoDB collection chỉ INSERT (`insert()`), KHÔNG có API update/delete. Nếu cần "sửa lỗi", ghi thêm log mới với metadata chỉ rõ log cũ bị sửa. Application user chỉ có quyền `insert` + `find`, KHÔNG có quyền `update` + `remove`. |
| 2 | **Structured** (Có cấu trúc) | AuditEvent là structured DTO với schema rõ ràng. Lưu vào MongoDB dạng BSON document có cấu trúc nhất quán, không phải free-text. Hỗ trợ query, aggregate, filter. |
| 3 | **Contextual** (Có ngữ cảnh) | Mỗi audit entry chứa đầy đủ: **Who** (actor.id, actor.type, actor.email, actor.ip), **What** (action, entity.type, entity.id), **When** (timestamp), **Where** (serviceName), **Why** (description, errorMessage), **How** (oldValue → newValue). |
| 4 | **Non-blocking** (Không chặn) | Ghi audit event qua Outbox Pattern (cùng transaction, nhưng publish async qua Outbox Relay). Nếu Kafka/MongoDB down, business logic vẫn hoạt động bình thường — audit event chờ trong outbox. |
| 5 | **Tenant-aware** | Mọi audit document chứa `tenantId`. API query tự động filter theo `tenantId` từ JWT. Owner chỉ xem được log của tenant mình. |
| 6 | **Retention rõ ràng** | TTL = 12 tháng, enforce bởi MongoDB TTL Index (tự động xóa). |

---

# 12. Kế hoạch triển khai

## 12.1. Thứ tự triển khai đề xuất

| Bước | Nội dung | Phụ thuộc |
|------|---------|-----------|
| 1 | Thêm MongoDB vào docker-compose (audit-db) | Không |
| 2 | Tạo Audit Service (Spring Boot + MongoDB + Kafka consumer) | Bước 1 |
| 3 | Tạo Kafka topic `audit-log-events` | Không |
| 4 | Tạo AuditEvent DTO + `saveAuditOutboxEvent()` helper ở **1 service đầu tiên** (đề xuất: Booking Service — có nhiều sự kiện nhất) | Bước 2 |
| 5 | Test end-to-end: Booking action → outbox → Kafka → Audit Service → MongoDB → API query | Bước 4 |
| 6 | Mở rộng sang các service còn lại (Payment, User, Hotel, Promotion, Notification) | Bước 5 |
| 7 | (Tùy chọn) Tạo shared library nếu thấy cần | Sau bước 6 |
| 8 | Kết nối UI Admin Console hiển thị audit trail | Bước 5 |

## 12.2. Docker Compose bổ sung

```yaml
  # === Audit Service ===
  audit-db:
    image: mongo:7.0
    restart: unless-stopped
    ports:
      - "${AUDIT_DB_EXTERNAL_PORT:-27017}:27017"
    environment:
      MONGO_INITDB_DATABASE: audit_db
    volumes:
      - audit-db-data:/data/db
    healthcheck:
      test: ["CMD", "mongosh", "--eval", "db.adminCommand('ping')"]
      interval: 10s
      timeout: 5s
      retries: 10
    networks:
      - app-network

  audit-service:
    build: ./services/audit-service
    ports:
      - "${AUDIT_SERVICE_PORT:-5008}:5000"
    environment:
      SERVER_PORT: 5000
      MONGO_URI: mongodb://audit-db:27017/audit_db
      KAFKA_BOOTSTRAP_SERVERS: ${KAFKA_BOOTSTRAP_SERVERS:-kafka:29092}
      KAFKA_CONSUMER_GROUP: audit-service-group
      EUREKA_ENABLED: ${EUREKA_ENABLED:-true}
      EUREKA_SERVER_URL: ${EUREKA_SERVER_URL:-http://eureka-server:8761/eureka}
    depends_on:
      audit-db:
        condition: service_healthy
      kafka:
        condition: service_healthy
      eureka-server:
        condition: service_healthy
    networks:
      - app-network
    volumes:
      - ./services/audit-service:/app
      - maven-cache:/root/.m2

# Thêm volume:
volumes:
  audit-db-data:
```

## 12.3. Cấu trúc thư mục Audit Service

```
services/audit-service/
├── pom.xml
├── Dockerfile
└── src/main/
    ├── java/com/audit_service/
    │   ├── AuditServiceApplication.java
    │   ├── config/
    │   │   ├── MongoConfig.java
    │   │   └── KafkaConsumerConfig.java
    │   ├── consumer/
    │   │   └── AuditLogConsumer.java          # @KafkaListener
    │   ├── document/
    │   │   └── AuditLogDocument.java          # MongoDB @Document
    │   ├── dto/
    │   │   ├── AuditEvent.java                # Inbound DTO (from Kafka)
    │   │   ├── AuditLogResponse.java          # Outbound DTO (API response)
    │   │   └── AuditLogStatistics.java
    │   ├── repository/
    │   │   └── AuditLogRepository.java        # MongoRepository
    │   ├── service/
    │   │   └── AuditLogService.java           # Business logic (query, stats)
    │   └── controller/
    │       └── AuditLogController.java        # REST API
    └── resources/
        └── application.properties
```
