# Hướng dẫn cài đặt Promotion Service cho AGENTS

> **Đọc kỹ toàn bộ file này trước khi bắt đầu code.**
> File tham chiếu chính:
> - [`docs/ad_promotion_service.md`](docs/ad_promotion_service.md) — thiết kế DB, API, luồng
> - [`docs/kafka-events.md`](docs/kafka-events.md) — schema đầy đủ mọi event/command
> - [`docs/ad_booking_service.md`](docs/ad_booking_service.md) — sequence diagram luồng Saga
> - [`docs/ad_place_booking_service.md`](docs/ad_place_booking_service.md) — thiết kế place-booking-service

---

## Tổng quan mục tiêu

Cần cài đặt **4 nhóm tính năng** sau:

| # | Nhóm | Service cần sửa |
|---|------|-----------------|
| 1 | Tích hợp Promotion vào luồng đặt phòng (pre-saga validate + in-saga validate) | `place-booking-service`, `promotion-service` |
| 2 | API lấy thông tin coupon theo code (dùng cho FE hiển thị trước khi đặt) | `promotion-service` |
| 3 | Sửa bảng `bookings` của booking-service | `booking-service` |
| 4 | Sửa frontend CheckoutPage | `frontend` |

---

## Kiến trúc tổng thể — Hai lần validate promotion

Hệ thống validate promotion **hai lần** với mục đích khác nhau:

```
Lần 1 — PRE-SAGA (đồng bộ, gọi HTTP):
  PlaceBookingController nhận request
  → Gọi HTTP đến promotion-service: validatePromotion(couponCode, totalAmount, ...)
  → Nếu KHÔNG hợp lệ: trả lỗi ngay cho FE (4xx), KHÔNG khởi động Saga
  → Nếu HỢP LỆ: tiếp tục khởi động Saga

Lần 2 — IN-SAGA (bất đồng bộ, qua Kafka):
  PBS gửi ValidatePromotion command → promotion-service
  → Nếu KHÔNG hợp lệ: PBS gửi SendBookingFailed → notification-service → dừng Saga
  → Nếu HỢP LỆ: PBS gửi CreateBooking → tiếp tục Saga bình thường
```

**Lý do cần validate hai lần:**
- Lần 1 (pre-saga): Trả phản hồi nhanh, tránh tốn tài nguyên khởi động Saga nếu coupon rõ ràng không hợp lệ (hết hạn, sai code, v.v.).
- Lần 2 (in-saga): Chặn race condition thật sự — khoảng thời gian giữa lần 1 và lần 2, coupon có thể bị người khác dùng hết lượt. Lần này mới thực sự **reserve (lock) lượt dùng** của coupon.

---

## Phần 1 — Tích hợp Promotion vào luồng đặt phòng

### 1.1. Luồng Saga đầy đủ

**Luồng hiện tại** (không có Promotion):
```
POST /place-booking → PlaceBookingController
  (check duplicate, validate user/hotel/room, check availability sơ bộ)
  → startSaga() → outbox: CreateBooking
  → BS giữ phòng → BookingCreated
  → PBS → ProcessPayment → PaymentSucceeded
  → PBS → ConfirmBooking → BookingConfirmed
  → PBS → SendBookingConfirmed
```

**Luồng mới** khi request **có `couponCode`**:

```
POST /place-booking → PlaceBookingController
  (check duplicate, validate user/hotel/room, check availability sơ bộ)
  
  [BƯỚC MỚI - PRE-SAGA] HTTP GET /api/promotions/validate-pre
    → promotion-service kiểm tra nhanh (không reserve)
    → Nếu lỗi: trả 4xx cho FE ngay, DỪNG
    → Nếu OK: trả { discountAmount, finalAmount }

  → startSaga() với finalAmount đã tính → outbox: ValidatePromotion

  [IN-SAGA] promotion-service consume ValidatePromotion
    → Reserve lượt dùng (SELECT FOR UPDATE, INSERT RESERVED)
    → Nếu thất bại: publish PromotionRejected
    → Nếu thành công: publish PromotionValidated { discountAmount, finalAmount }

  PBS consume PromotionRejected:
    → Cập nhật SagaState = FAILED
    → outbox: SendBookingFailed → NS thông báo FE → DỪNG Saga

  PBS consume PromotionValidated:
    → outbox: CreateBooking { originalAmount, finalAmount }

  BS tạo booking PENDING { original_amount, final_amount }
  → BookingCreated
  → PBS → ProcessPayment (amount = finalAmount)
  → PaymentSucceeded
  → PBS → ConfirmPromotionUsage (nếu có promotion)
  → PromotionUsageConfirmed
  → PBS → ConfirmBooking → BookingConfirmed
  → PBS → SendBookingConfirmed
```

**Khi rollback** (BookingFailed hoặc PaymentFailed, sau khi đã ValidatePromotion thành công):
```
  → PBS gửi ReleasePromotionUsage (compensating transaction)
  → promotion-service đặt PromotionUsage.status = CANCELLED
  → PBS gửi SendBookingFailed
```

**Lưu ý quan trọng về `couponCode` trong SagaState:**
- **KHÔNG** lưu `promotionId`, `couponId`, `discountAmount`, `finalAmount` vào `saga_states`.
- **CHỈ** lưu `couponCode` (string mã coupon user nhập) — để phục vụ idempotency và logging.
- Promotion-service tự biết `promotionId`/`couponId` qua `bookingId` trong bảng `promotion_usages`.
- Khi PBS cần gửi `ConfirmPromotionUsage` hoặc `ReleasePromotionUsage`, chỉ cần truyền `bookingId` và `couponCode` — promotion-service tự tìm usage record theo `bookingId`.

---

### 1.2. Kafka Topics và Events

Thêm 2 topics mới vào hệ thống. Xem schema đầy đủ tại [`docs/kafka-events.md`](docs/kafka-events.md).

**Topic `promotion-commands`** — PBS publish, Promotion-Service consume:

| Event | Khi nào gửi | Các trường quan trọng |
|-------|-------------|----------------------|
| `ValidatePromotion` | Đầu Saga (sau pre-validate đã pass), trước `CreateBooking` | `sagaId`, `bookingId`, `userId`, `couponCode`, `hotelId`, `roomTypeIds`, `checkin`, `checkout`, `totalAmount` |
| `ConfirmPromotionUsage` | Sau `PaymentSucceeded` | `sagaId`, `bookingId`, `couponCode`, `userId` |
| `ReleasePromotionUsage` | Khi `BookingFailed` hoặc `PaymentFailed` (compensating) | `sagaId`, `bookingId`, `couponCode`, `reason` |

**Topic `promotion-events`** — Promotion-Service publish, PBS consume:

| Event | Ý nghĩa | Các trường quan trọng |
|-------|---------|----------------------|
| `PromotionValidated` | Reserve thành công, coupon hợp lệ | `sagaId`, `bookingId`, `promotionId`, `couponId`, `discountAmount`, `finalAmount` |
| `PromotionRejected` | Coupon không hợp lệ hoặc hết lượt | `sagaId`, `bookingId`, `reason` |
| `PromotionUsageConfirmed` | Confirm usage thành công | `sagaId`, `bookingId` |
| `PromotionUsageFailed` | Confirm usage thất bại | `sagaId`, `bookingId`, `reason` |

---

### 1.3. Thay đổi trong `promotion-service`

#### 1.3.1. API pre-validate (đồng bộ, KHÔNG reserve)

Thêm endpoint mới vào [`PromotionController.java`](services/promotion-service/src/main/java/com/promotion/promotion_service/controller/PromotionController.java):

```java
/**
 * Validate nhanh trước khi bắt đầu Saga.
 * KHÔNG reserve lượt dùng. Chỉ kiểm tra coupon/promotion có hợp lệ không.
 * Được gọi bằng HTTP đồng bộ từ place-booking-service (Feign Client).
 */
@PostMapping("/validate-pre")
public ValidatePromotionPreResponse validatePre(
        @Valid @RequestBody ValidatePromotionPreRequest request) {
    return promotionService.validatePre(request);
}
```

**`ValidatePromotionPreRequest`** (tạo trong `dto/request`):
```java
public class ValidatePromotionPreRequest {
    private String couponCode;      // mã coupon
    private UUID userId;            // để check per-user limit
    private UUID hotelId;           // để check scope
    private List<UUID> roomTypeIds;
    private String checkin;
    private String checkout;
    private BigDecimal totalAmount; // để tính discountAmount
}
```

**`ValidatePromotionPreResponse`** (tạo trong `dto/response`):
```java
public class ValidatePromotionPreResponse {
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String promotionName;
    private String discountDescription; // "Giảm 20%" hoặc "Giảm 50,000 VNĐ"
}
```

**Logic `validatePre`** (trong `PromotionServiceImpl`):
```
1. Tìm Coupon theo code (status=ACTIVE, is_deleted=false) — nếu không thấy: ném exception
2. Load Promotion liên kết (status=ACTIVE, start_at <= now <= end_at) — nếu không thỏa: ném exception
3. Check scope (hotelId, roomTypeIds) — nếu không áp dụng: ném exception
4. Check min_booking_amount, min_nights
5. Check usage limit (đọc thông thường, KHÔNG lock, KHÔNG reserve)
   - Kiểm tra coupon.currentUsageCount < coupon.usageLimit
   - Kiểm tra promotion.currentUsageCount < promotion.totalUsageLimit
   - Kiểm tra per-user: đếm promotion_usages WHERE user_id=? AND promotion_id=? AND status IN (RESERVED, CONFIRMED)
6. Tính discountAmount và finalAmount
7. Trả ValidatePromotionPreResponse
```

> **Lưu ý:** Đây là snapshot tại thời điểm check, có thể stale (người khác dùng xong trong lúc này). Đó là lý do cần validate lại trong Saga với lock.

Khi validation thất bại, ném exception với HTTP status phù hợp:
- Coupon không tồn tại / hết hạn: `404 Not Found` hoặc `422 Unprocessable Entity`
- Coupon hết lượt dùng: `409 Conflict`
- Không đủ điều kiện (số tiền, số đêm): `422 Unprocessable Entity`

#### 1.3.2. API lấy thông tin coupon (cho FE hiển thị)

Thêm endpoint vào `PromotionController.java`:

```java
/**
 * Lấy thông tin coupon theo code để FE hiển thị trước khi đặt.
 * KHÔNG validate đầy đủ, KHÔNG reserve. Chỉ trả thông tin coupon/promotion.
 */
@GetMapping("/coupons/{code}")
public CouponDetailResponse getCouponByCode(
        @PathVariable String code,
        @RequestParam(required = false) BigDecimal totalAmount,
        @RequestParam(required = false) UUID hotelId) {
    return promotionService.getCouponDetail(code, totalAmount, hotelId);
}
```

**`CouponDetailResponse`** bao gồm:
- `couponId`, `code`, `status`, `usageLimit`, `currentUsageCount`, `remainingUsages`
- `promotionId`, `promotionName`, `discountType`, `discountValue`, `maxDiscountAmount`
- `discountAmount` (tính theo `totalAmount` nếu được truyền, else `null`)
- `finalAmount` (tính theo `totalAmount` nếu được truyền, else `null`)

#### 1.3.3. Thêm Kafka Consumer

Tạo file mới: `services/promotion-service/src/main/java/com/promotion/promotion_service/service/kafka/PromotionKafkaConsumerService.java`

Tham khảo pattern từ [`KafkaConsumerService.java`](services/place_booking_service/src/main/java/com/place_booking_service/service/kafka/KafkaConsumerService.java) của PBS.

```java
@Service
@RequiredArgsConstructor
@Transactional
public class PromotionKafkaConsumerService {

    @KafkaListener(topics = "promotion-commands", groupId = "promotion-service")
    public void promotionCommandsHandler(String payloadJson) throws JsonProcessingException {
        Map<String, Object> payload = new ObjectMapper().readValue(payloadJson, Map.class);
        String eventType = (String) payload.get("eventType");

        ObjectMapper mapper = new ObjectMapper();
        switch (eventType) {
            case "ValidatePromotion"     -> handleValidatePromotion(mapper.convertValue(payload, ValidatePromotionCommand.class));
            case "ConfirmPromotionUsage" -> handleConfirmPromotionUsage(mapper.convertValue(payload, ConfirmPromotionUsageCommand.class));
            case "ReleasePromotionUsage" -> handleReleasePromotionUsage(mapper.convertValue(payload, ReleasePromotionUsageCommand.class));
        }
    }
}
```

#### 1.3.4. Logic `handleValidatePromotion` (IN-SAGA — có reserve)

Đây là bước quan trọng nhất trong promotion-service. Thực hiện **trong một transaction**:

```
1. Idempotency check: kiểm tra promotion_usages.idempotency_key = sagaId + ":" + bookingId
   - Nếu đã tồn tại (status=RESERVED hoặc CONFIRMED): publish lại PromotionValidated (idempotent)
   - Nếu status=CANCELLED: coi như mới, tiếp tục

2. Tìm Coupon theo code (cùng logic với validatePre)

3. Load Promotion liên kết

4. Check scope, min_amount, min_nights (cùng logic validatePre)

5. *** CHỐNG RACE CONDITION — LOCK ***
   a. Lock Coupon: SELECT FOR UPDATE trên CouponEntity theo id
   b. Kiểm tra coupon.currentUsageCount + COUNT(RESERVED by this booking) < coupon.usageLimit
   c. Lock Promotion: SELECT FOR UPDATE trên PromotionEntity theo id
   d. Kiểm tra promotion.currentUsageCount + COUNT(RESERVED) < promotion.totalUsageLimit
   e. Kiểm tra per-user limit: COUNT(promotion_usages WHERE user_id=? AND promotion_id=? AND status IN (RESERVED,CONFIRMED))

6. Tính discountAmount và finalAmount

7. INSERT promotion_usages:
   - status = RESERVED
   - idempotency_key = sagaId + ":" + bookingId
   - discount_amount = discountAmount
   - booking_amount = totalAmount (từ command)
   - used_at = now()

8. Publish PromotionValidated { sagaId, bookingId, promotionId, couponId, discountAmount, finalAmount }
   (qua outbox — trong cùng transaction)

Nếu bất kỳ bước nào thất bại:
→ Publish PromotionRejected { sagaId, bookingId, reason }
```

> **Quan trọng — Cơ chế lock:**
> Dùng `@Lock(LockModeType.PESSIMISTIC_WRITE)` trong Repository (PostgreSQL `SELECT FOR UPDATE`).
> Tham khảo pattern lock của booking-service trong [`docs/ad_booking_service.md`](docs/ad_booking_service.md) mục 3.1.
> Ở promotion-service KHÔNG cần Redis lock (lượt dùng ít hơn phòng khách sạn), chỉ cần DB-level lock là đủ.

Repository methods cần thêm:
```java
// CouponRepository
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT c FROM CouponEntity c WHERE c.code = :code AND c.isDeleted = false")
Optional<CouponEntity> findByCodeForUpdate(@Param("code") String code);

// PromotionRepository
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM PromotionEntity p WHERE p.id = :id AND p.isDeleted = false")
Optional<PromotionEntity> findByIdForUpdate(@Param("id") UUID id);
```

#### 1.3.5. Logic `handleConfirmPromotionUsage`

```
1. Tìm PromotionUsage theo bookingId (status = RESERVED)
   - Nếu không tìm thấy: publish PromotionUsageFailed
   
2. Trong transaction với SELECT FOR UPDATE:
   a. Cập nhật PromotionUsage.status → CONFIRMED
   b. Tăng promotion.currentUsageCount += 1
   c. Tăng coupon.currentUsageCount += 1 (nếu có coupon)
   
3. Publish PromotionUsageConfirmed { sagaId, bookingId }
```

Tìm usage theo bookingId (không cần couponCode/promotionId — đã lưu trong bảng):
```java
// PromotionUsageRepository
Optional<PromotionUsageEntity> findByBookingIdAndStatus(UUID bookingId, PromotionUsageStatus status);
```

#### 1.3.6. Logic `handleReleasePromotionUsage`

```
1. Tìm PromotionUsage theo bookingId (status = RESERVED)
   - Nếu không tìm thấy: log warn, không làm gì (idempotent)
   
2. Cập nhật PromotionUsage.status → CANCELLED
   - KHÔNG tăng/giảm currentUsageCount (currentUsageCount chỉ tăng khi CONFIRM)
   
3. Không cần publish event reply
```

#### 1.3.7. Tạo DTO cho Kafka

Tạo trong package `com.promotion.promotion_service.dto.kafka`:

**Commands (nhận từ PBS):**
```java
// ValidatePromotionCommand.java
public class ValidatePromotionCommand {
    private String eventType;
    private UUID sagaId;
    private UUID bookingId;
    private UUID userId;
    private String couponCode;
    private UUID hotelId;
    private List<UUID> roomTypeIds;
    private String checkin;
    private String checkout;
    private BigDecimal totalAmount;
}

// ConfirmPromotionUsageCommand.java
public class ConfirmPromotionUsageCommand {
    private String eventType;
    private UUID sagaId;
    private UUID bookingId;
    private String couponCode;
    private UUID userId;
}

// ReleasePromotionUsageCommand.java
public class ReleasePromotionUsageCommand {
    private String eventType;
    private UUID sagaId;
    private UUID bookingId;
    private String couponCode;
    private String reason;
}
```

**Events (gửi cho PBS):**
```java
// PromotionValidatedEvent.java
public class PromotionValidatedEvent {
    private String eventType = "PromotionValidated";
    private UUID sagaId;
    private UUID bookingId;
    private UUID promotionId;
    private UUID couponId;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
}

// PromotionRejectedEvent.java
public class PromotionRejectedEvent {
    private String eventType = "PromotionRejected";
    private UUID sagaId;
    private UUID bookingId;
    private String reason;
}

// PromotionUsageConfirmedEvent.java / PromotionUsageFailedEvent.java
// (tương tự, theo schema kafka-events.md)
```

#### 1.3.8. Cấu hình Kafka Consumer trong promotion-service

Thêm vào `application.yml`:
```yaml
spring:
  kafka:
    consumer:
      group-id: promotion-service
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
```

Thêm `@EnableKafka` vào config class nếu chưa có.

---

### 1.4. Thay đổi trong `place-booking-service`

#### 1.4.1. Thêm Feign Client gọi promotion-service

Tạo file mới: `services/place_booking_service/src/main/java/com/place_booking_service/client/PromotionServiceClient.java`

Tham khảo pattern từ [`HotelServiceClient.java`](services/place_booking_service/src/main/java/com/place_booking_service/client/HotelServiceClient.java):

```java
@FeignClient(name = "promotion-service")
public interface PromotionServiceClient {

    /**
     * Pre-validate coupon trước khi bắt đầu Saga.
     * Trả về discountAmount và finalAmount nếu hợp lệ.
     * Ném FeignException với HTTP 4xx nếu không hợp lệ.
     */
    @PostMapping("/api/promotions/validate-pre")
    ValidatePromotionPreResponse validatePromotion(
            @RequestBody ValidatePromotionPreRequest request);
}
```

Tạo DTO tương ứng trong package `com.place_booking_service.dto`:
- `ValidatePromotionPreRequest.java`
- `ValidatePromotionPreResponse.java`

#### 1.4.2. Thêm `couponCode` vào `PlaceBookingRequest`

File: [`PlaceBookingRequest.java`](services/place_booking_service/src/main/java/com/place_booking_service/dto/PlaceBookingRequest.java)

```java
private String couponCode; // nullable — không bắt buộc
```

#### 1.4.3. Sửa `PlaceBookingController` — thêm Bước pre-validate

File: [`PlaceBookingController.java`](services/place_booking_service/src/main/java/com/place_booking_service/controller/PlaceBookingController.java)

Inject thêm `PromotionServiceClient`. Thêm **Bước 6 mới** sau Bước 5 (check phòng trống) và trước Bước 6 cũ (startSaga):

```java
// Inject
PromotionServiceClient promotionServiceClient;

// Trong placeBooking():

// *** BƯỚC 6 MỚI: Pre-validate promotion (nếu có couponCode) ***
// Nếu invalid → Feign ném exception → GlobalExceptionHandler trả 4xx cho FE
// Nếu valid → lấy finalAmount để truyền xuống startSaga
BigDecimal finalAmount = placeBookingRequest.getTotalAmount(); // mặc định = totalAmount

if (placeBookingRequest.getCouponCode() != null
        && !placeBookingRequest.getCouponCode().isBlank()) {

    ValidatePromotionPreRequest preRequest = new ValidatePromotionPreRequest();
    preRequest.setCouponCode(placeBookingRequest.getCouponCode());
    preRequest.setUserId(placeBookingRequest.getUserId());
    preRequest.setHotelId(placeBookingRequest.getHotelId());
    preRequest.setRoomTypeIds(
            placeBookingRequest.getRoomTypeList().stream()
                    .map(r -> r.getRoomTypeId())
                    .toList());
    preRequest.setCheckin(placeBookingRequest.getCheckin());
    preRequest.setCheckout(placeBookingRequest.getCheckout());
    preRequest.setTotalAmount(placeBookingRequest.getTotalAmount());

    // Nếu promotion-service trả 4xx → FeignException được ném ra
    // → GlobalExceptionHandler sẽ map thành response lỗi cho FE
    ValidatePromotionPreResponse preResponse =
            promotionServiceClient.validatePromotion(preRequest);

    finalAmount = preResponse.getFinalAmount();
}

// *** BƯỚC 7: Khởi tạo Saga ***
bookingId = placeBookingService.startSaga(
        placeBookingRequest,
        user,
        hotelAndRoomTypes.hotel(),
        bookingId,
        hashRequest,
        finalAmount);   // <-- truyền thêm finalAmount
```

> **Xử lý lỗi từ FeignException:** Cần thêm `FeignExceptionHandler` vào `GlobalExceptionHandler` để map HTTP status từ promotion-service sang response phù hợp cho FE (ví dụ: 409 từ promotion-service → trả 409 cho FE với message "Coupon đã hết lượt dùng").

#### 1.4.4. Thêm `couponCode` vào `SagaState` — CHỈ trường này

File: [`SagaState.java`](services/place_booking_service/src/main/java/com/place_booking_service/entity/SagaState.java)

Thêm **đúng 1 trường** (không thêm promotionId/couponId/discountAmount/finalAmount):

```java
// Mã coupon user nhập (null nếu không dùng coupon)
private String couponCode;
```

**Không cần thêm** `promotionId`, `couponId`, `discountAmount`, `finalAmount` vào SagaState.

#### 1.4.5. Sửa `PlaceBookingService.startSaga()`

File: [`PlaceBookingService.java`](services/place_booking_service/src/main/java/com/place_booking_service/service/PlaceBookingService.java)

Thêm tham số `finalAmount` và `couponCode` vào method signature:

```java
@Transactional
public UUID startSaga(PlaceBookingRequest request, User user, HotelSummaryResponse hotel,
        UUID bookingId, String hashRequest, BigDecimal finalAmount) {

    // ... giữ nguyên phần idempotency check hiện tại ...

    // Lưu couponCode vào saga (để biết saga này có dùng promotion không)
    sagaState.setCouponCode(request.getCouponCode());

    // NẾU có couponCode: gửi ValidatePromotion (in-saga)
    if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
        sagaState.setCurrentStep("VALIDATING_PROMOTION");
        sagaStateRepository.save(sagaState);

        ValidatePromotion validateCmd = buildValidatePromotionCommand(
                request, user, bookingId, sagaId);
        outboxPublisherService.saveOutboxMessage(
                "promotion-commands", validateCmd, "ValidatePromotion");
    } else {
        // Không có coupon → gửi CreateBooking trực tiếp (flow hiện tại)
        sagaState.setCurrentStep("STARTED");
        sagaStateRepository.save(sagaState);

        CreateBooking createBooking = buildCreateBookingCommand(
                request, user, hotel, bookingId, sagaId,
                request.getTotalAmount(),  // originalAmount
                request.getTotalAmount()); // finalAmount = originalAmount (không giảm)
        outboxPublisherService.saveOutboxMessage(
                "booking-commands", createBooking, "CreateBooking");
    }

    return bookingId;
}

private ValidatePromotion buildValidatePromotionCommand(
        PlaceBookingRequest request, User user, UUID bookingId, UUID sagaId) {
    ValidatePromotion cmd = new ValidatePromotion();
    cmd.setEventType("ValidatePromotion");
    cmd.setSagaId(sagaId);
    cmd.setBookingId(bookingId);
    cmd.setUserId(request.getUserId());
    cmd.setCouponCode(request.getCouponCode());
    cmd.setHotelId(request.getHotelId());
    cmd.setRoomTypeIds(request.getRoomTypeList().stream()
            .map(r -> r.getRoomTypeId()).toList());
    cmd.setCheckin(request.getCheckin());
    cmd.setCheckout(request.getCheckout());
    cmd.setTotalAmount(request.getTotalAmount());
    return cmd;
}
```

**Lưu ý:** `buildCreateBookingCommand` cần nhận thêm `originalAmount` và `finalAmount` để truyền sang booking-service.

#### 1.4.6. Tạo các DTO mới trong place-booking-service

Tạo trong package `com.place_booking_service.dto`:

```
ValidatePromotion.java          — command gửi sang promotion-service (Kafka)
PromotionValidated.java         — event nhận từ promotion-service (Kafka)
PromotionRejected.java          — event nhận từ promotion-service (Kafka)
ConfirmPromotionUsage.java      — command gửi sang promotion-service (Kafka)
ReleasePromotionUsage.java      — command gửi sang promotion-service (Kafka)
PromotionUsageConfirmed.java    — event nhận từ promotion-service (Kafka)
PromotionUsageFailed.java       — event nhận từ promotion-service (Kafka)
ValidatePromotionPreRequest.java  — HTTP request đến promotion-service (Feign)
ValidatePromotionPreResponse.java — HTTP response từ promotion-service (Feign)
```

Tham khảo pattern từ [`CreateBooking.java`](services/place_booking_service/src/main/java/com/place_booking_service/dto/CreateBooking.java) và [`BookingCreated.java`](services/place_booking_service/src/main/java/com/place_booking_service/dto/BookingCreated.java).

#### 1.4.7. Sửa `KafkaConsumerService` — thêm listener `promotion-events`

File: [`KafkaConsumerService.java`](services/place_booking_service/src/main/java/com/place_booking_service/service/kafka/KafkaConsumerService.java)

Thêm listener mới theo đúng pattern của `bookingEventsHandler` và `paymentEventsHandler`:

```java
@KafkaListener(topics = "promotion-events")
@Transactional
public void promotionEventsHandler(String payloadJson) throws JsonProcessingException {
    Map<String, Object> payload = new ObjectMapper().readValue(payloadJson, Map.class);
    String eventType = (String) payload.get("eventType");
    UUID sagaId = UUID.fromString((String) payload.get("sagaId"));

    Optional<SagaState> state = sagaStateRepository.findSagaStateById(sagaId);
    if (state.isEmpty()) return;

    ObjectMapper mapper = new ObjectMapper();
    switch (eventType) {
        case "PromotionValidated"      -> handlePromotionValidated(mapper.convertValue(payload, PromotionValidated.class));
        case "PromotionRejected"       -> handlePromotionRejected(mapper.convertValue(payload, PromotionRejected.class));
        case "PromotionUsageConfirmed" -> handlePromotionUsageConfirmed(mapper.convertValue(payload, PromotionUsageConfirmed.class));
        case "PromotionUsageFailed"    -> handlePromotionUsageFailed(mapper.convertValue(payload, PromotionUsageFailed.class));
    }
}
```

**Implement các handler:**

```java
private void handlePromotionValidated(PromotionValidated event) {
    Optional<SagaState> sagaOpt = sagaStateRepository.findByBookingId(event.getBookingId());
    if (sagaOpt.isEmpty()) return;

    SagaState saga = sagaOpt.get();
    saga.setCurrentStep("PROMOTION_VALIDATED");
    saga.setUpdatedAt(LocalDateTime.now());
    sagaStateRepository.save(saga);

    // Tiếp tục Saga — gửi CreateBooking với finalAmount sau giảm giá
    // finalAmount và originalAmount lấy từ event.getFinalAmount() và event.getTotalAmount()
    // (PBS cần rebuild CreateBooking command — dùng pending_payload hoặc lấy lại từ saga)
    CreateBooking createBooking = buildCreateBookingFromPromotionValidated(saga, event);
    outboxPublisherService.saveOutboxMessage("booking-commands", createBooking, "CreateBooking");
}

private void handlePromotionRejected(PromotionRejected event) {
    Optional<SagaState> sagaOpt = sagaStateRepository.findByBookingId(event.getBookingId());
    if (sagaOpt.isEmpty()) return;

    SagaState saga = sagaOpt.get();
    saga.setStatus("FAILED");
    saga.setCurrentStep("PROMOTION_REJECTED");
    saga.setUpdatedAt(LocalDateTime.now());
    sagaStateRepository.save(saga);

    // Gửi thông báo thất bại đến user (tương tự handleBookingFailed)
    // Cần có thông tin user/email để gửi — lấy từ saga hoặc gọi user-service
    SendBookingFailed sendFailed = new SendBookingFailed();
    sendFailed.setEventType("SendBookingFailed");
    sendFailed.setSagaId(saga.getId());
    sendFailed.setBookingId(event.getBookingId());
    sendFailed.setReason(event.getReason()); // "Coupon đã hết lượt dùng" v.v.
    // setTo(...) — cần email của user: lấy từ SagaState.userEmail hoặc gọi user-service
    outboxPublisherService.saveOutboxMessage("notification-commands", sendFailed, "SendBookingFailed");
    // DỪNG SAGA tại đây — không gửi thêm bất kỳ command nào
}

private void handlePromotionUsageConfirmed(PromotionUsageConfirmed event) {
    Optional<SagaState> sagaOpt = sagaStateRepository.findByBookingId(event.getBookingId());
    if (sagaOpt.isEmpty()) return;

    SagaState saga = sagaOpt.get();
    saga.setCurrentStep("PROMOTION_USAGE_CONFIRMED");
    saga.setUpdatedAt(LocalDateTime.now());
    sagaStateRepository.save(saga);

    // Tiếp tục gửi ConfirmBooking
    ConfirmBooking confirmBooking = new ConfirmBooking();
    confirmBooking.setBookingId(event.getBookingId());
    confirmBooking.setEventType("ConfirmBooking");
    confirmBooking.setSagaId(saga.getId());
    outboxPublisherService.saveOutboxMessage("booking-commands", confirmBooking, "ConfirmBooking");
}

private void handlePromotionUsageFailed(PromotionUsageFailed event) {
    // Log error, cập nhật saga FAILED
    // Cân nhắc: rollback booking (gửi CancelBooking) hoặc chỉ log tùy business rule
}
```

**⚠️ Vấn đề cần giải quyết — `handlePromotionValidated` cần build lại `CreateBooking`:**

Khi PBS nhận `PromotionValidated`, cần gửi `CreateBooking` command kèm đủ thông tin (user, hotel, roomTypes, ngày, finalAmount). Vì không muốn lưu quá nhiều vào SagaState, khuyến nghị thêm **1 trường** vào `saga_states`:

```java
// Thêm vào SagaState (ngoài couponCode đã thêm ở trên)
@Column(columnDefinition = "TEXT")
private String pendingPayload; // JSON của CreateBooking command (chưa có sagaId, finalAmount)
```

**Cách dùng:**
1. Trong `startSaga()`, nếu có couponCode: serialize `CreateBooking` (với `totalAmount` gốc) thành JSON, lưu vào `saga.pendingPayload`.
2. Trong `handlePromotionValidated()`: deserialize `pendingPayload`, cập nhật `totalAmount = event.getFinalAmount()`, thêm `sagaId`, rồi gửi.

**Cách implement:**
```java
private void handlePromotionValidated(PromotionValidated event) {
    // ...
    CreateBooking createBooking = objectMapper.readValue(saga.getPendingPayload(), CreateBooking.class);
    createBooking.setSagaId(saga.getId());
    createBooking.setTotalAmount(event.getFinalAmount());     // giá sau giảm
    createBooking.setOriginalAmount(createBooking.getTotalAmount()); // giá gốc (lưu trước khi ghi đè)
    // ... sau đó lưu outbox
}
```

> Cách này tốt hơn lưu từng trường vì không cần thêm nhiều cột vào `saga_states`.

#### 1.4.8. Sửa `handlePaymentSucceeded` — gửi ConfirmPromotionUsage trước ConfirmBooking

```java
private void handlePaymentSucceeded(PaymentSucceeded paymentSucceeded) {
    // ... cập nhật saga status PAYMENT_SUCCEEDED ...

    SagaState saga = sagaOpt.get();

    // Nếu có coupon → gửi ConfirmPromotionUsage trước
    if (saga.getCouponCode() != null && !saga.getCouponCode().isBlank()) {
        ConfirmPromotionUsage cmd = new ConfirmPromotionUsage();
        cmd.setEventType("ConfirmPromotionUsage");
        cmd.setSagaId(saga.getId());
        cmd.setBookingId(paymentSucceeded.getBookingId());
        cmd.setCouponCode(saga.getCouponCode());
        cmd.setUserId(saga.getUserId());
        outboxPublisherService.saveOutboxMessage("promotion-commands", cmd, "ConfirmPromotionUsage");
        // ConfirmBooking sẽ được gửi sau khi nhận PromotionUsageConfirmed
    } else {
        // Không có coupon → ConfirmBooking ngay (flow hiện tại)
        ConfirmBooking confirmBooking = new ConfirmBooking();
        confirmBooking.setBookingId(paymentSucceeded.getBookingId());
        confirmBooking.setEventType("ConfirmBooking");
        confirmBooking.setSagaId(saga.getId());
        outboxPublisherService.saveOutboxMessage("booking-commands", confirmBooking, "ConfirmBooking");
    }
}
```

#### 1.4.9. Sửa rollback handlers — thêm `ReleasePromotionUsage`

Thêm vào cuối `handleBookingFailed` và `handlePaymentFailed` (sau khi đã cập nhật saga status và gửi SendBookingFailed):

```java
// Thêm vào handleBookingFailed và handlePaymentFailed:
if (saga.getCouponCode() != null && !saga.getCouponCode().isBlank()) {
    // Chỉ release nếu saga đã qua bước PROMOTION_VALIDATED
    // (tức là promotion đã được reserved trong promotion_usages)
    if ("PROMOTION_VALIDATED".equals(saga.getCurrentStep())
            || "BOOKING_CREATED".equals(saga.getCurrentStep())
            || "PAYMENT_FAILED".equals(saga.getCurrentStep())) {
        ReleasePromotionUsage release = new ReleasePromotionUsage();
        release.setEventType("ReleasePromotionUsage");
        release.setSagaId(saga.getId());
        release.setBookingId(bookingId);
        release.setCouponCode(saga.getCouponCode());
        release.setReason("Booking/Payment failed");
        outboxPublisherService.saveOutboxMessage(
                "promotion-commands", release, "ReleasePromotionUsage");
    }
}
```

**Lý do chỉ release khi đã qua PROMOTION_VALIDATED:** Trước bước đó, promotion-service chưa insert record RESERVED → không cần release.

---

## Phần 2 — Sửa bảng `bookings` trong booking-service

### 2.1. Migration SQL

Đọc thiết kế hiện tại của bảng `bookings` tại [`docs/ad_booking_service.md`](docs/ad_booking_service.md).

```sql
-- Đổi tên cột total_amount → original_amount
ALTER TABLE bookings RENAME COLUMN total_amount TO original_amount;

-- Thêm cột final_amount (giá sau giảm giá coupon)
ALTER TABLE bookings ADD COLUMN final_amount NUMERIC;

-- Backfill: nếu không có coupon thì final_amount = original_amount
UPDATE bookings SET final_amount = original_amount WHERE final_amount IS NULL;

-- Đặt NOT NULL sau backfill
ALTER TABLE bookings ALTER COLUMN final_amount SET NOT NULL;
ALTER TABLE bookings ADD CONSTRAINT chk_final_amount_positive CHECK (final_amount > 0);
```

### 2.2. Sửa Entity `Booking` trong booking-service

Tìm `Booking.java` (hoặc `BookingEntity.java`) và sửa:

```java
@Column(name = "original_amount", nullable = false)
private BigDecimal originalAmount;  // giá gốc (trước giảm giá)

@Column(name = "final_amount", nullable = false)
private BigDecimal finalAmount;     // giá thực thu (sau giảm, bằng originalAmount nếu không có coupon)
```

### 2.3. Sửa `CreateBooking` command và handler trong booking-service

PBS truyền cả `originalAmount` và `finalAmount` trong `CreateBooking` command:
- `originalAmount` = `totalAmount` gốc của request
- `finalAmount` = sau khi trừ discount (= `originalAmount` nếu không có coupon)

Sửa class `CreateBooking.java` trong **place-booking-service** để thêm 2 trường này, và sửa handler `handleCreateBooking` trong **booking-service** để map đúng sang 2 cột.

---

## Phần 3 — Sửa Frontend (CheckoutPage)

### 3.1. API lấy thông tin coupon

Tạo file: `frontend/src/features/promotion/api/promotionApi.js`

```js
import axiosClient from '../../../shared/api/axiosClient';

/**
 * Lấy thông tin coupon để hiển thị (KHÔNG validate/reserve).
 * @param {string} code - mã coupon
 * @param {number} totalAmount - tổng tiền hiện tại để tính discountAmount
 * @param {string} hotelId - UUID khách sạn
 */
export async function fetchCouponDetail(code, totalAmount, hotelId) {
  const { data } = await axiosClient.get(`/promotions/coupons/${code}`, {
    params: { totalAmount, hotelId },
  });
  return data;
}
```

### 3.2. State management trong CheckoutPage

Thêm các state mới vào [`CheckoutPage.jsx`](frontend/src/pages/CheckoutPage.jsx):

```js
const [couponInput, setCouponInput]   = useState('');        // text user đang gõ
const [couponLoading, setCouponLoading] = useState(false);
const [appliedCoupon, setAppliedCoupon] = useState(null);
// appliedCoupon = { code, discountAmount, finalAmount, promotionName, remainingUsages } | null
const [couponError, setCouponError]   = useState('');

// finalPrice tính từ appliedCoupon
const finalPrice = appliedCoupon?.finalAmount ?? checkoutDraft?.price?.totalPrice;
```

### 3.3. Handler áp dụng coupon

```js
async function handleApplyCoupon() {
  if (!couponInput.trim()) return;
  setCouponLoading(true);
  setCouponError('');
  setAppliedCoupon(null);
  try {
    const res = await fetchCouponDetail(
      couponInput.trim(),
      checkoutDraft.price.totalPrice,
      checkoutDraft.hotel.hotelId
    );
    // res = { data: { code, discountAmount, finalAmount, promotionName, remainingUsages, ... } }
    setAppliedCoupon(res.data);
  } catch (err) {
    setCouponError(err.response?.data?.message || 'Mã coupon không hợp lệ.');
  } finally {
    setCouponLoading(false);
  }
}

function handleRemoveCoupon() {
  setAppliedCoupon(null);
  setCouponInput('');
  setCouponError('');
}
```

### 3.4. UI section "Mã giảm giá"

Thêm section này vào form, sau phần thông tin lưu trú và trước section "Tổng tiền":

```jsx
<section className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
  <h2 className="text-lg font-semibold text-slate-900">Mã giảm giá</h2>
  <div className="mt-4 flex gap-2">
    <input
      type="text"
      value={couponInput}
      onChange={(e) => setCouponInput(e.target.value.toUpperCase())}
      placeholder="Nhập mã coupon..."
      disabled={!!appliedCoupon || couponLoading}
      className="form-input flex-1"
    />
    {!appliedCoupon ? (
      <button
        type="button"
        onClick={handleApplyCoupon}
        disabled={couponLoading || !couponInput.trim()}
        className="accent-button"
      >
        {couponLoading ? <Spinner /> : 'Áp dụng'}
      </button>
    ) : (
      <button type="button" onClick={handleRemoveCoupon} className="text-sm text-red-600">
        Xóa mã
      </button>
    )}
  </div>

  {couponError && (
    <p className="mt-2 text-sm text-red-600">✗ {couponError}</p>
  )}

  {appliedCoupon && (
    <div className="mt-3 rounded-md bg-green-50 p-3 text-sm text-green-800">
      <p className="font-semibold">✓ {appliedCoupon.code} — {appliedCoupon.promotionName}</p>
      <p className="mt-1">Còn {appliedCoupon.remainingUsages} lượt sử dụng</p>
      <p>Giảm: {formatCurrency(appliedCoupon.discountAmount)}</p>
    </div>
  )}
</section>
```

### 3.5. Sửa section "Tổng tiền"

```jsx
<section className="rounded-lg border border-blue-200 bg-blue-50 p-5 shadow-sm">
  <h2 className="text-lg font-semibold text-blue-950">Tổng tiền</h2>
  <div className="mt-4 space-y-2 text-sm">
    <div className="flex justify-between">
      <span>Tạm tính</span>
      <span>{formatCurrency(checkoutDraft.price.totalPrice)}</span>
    </div>

    {appliedCoupon && (
      <div className="flex justify-between text-green-700">
        <span>Giảm giá ({appliedCoupon.code})</span>
        <span>−{formatCurrency(appliedCoupon.discountAmount)}</span>
      </div>
    )}

    <div className="flex justify-between border-t border-blue-200 pt-3 text-xl font-bold text-blue-950">
      <span>Thanh toán</span>
      <div className="text-right">
        {appliedCoupon && (
          <p className="text-sm font-normal text-slate-400 line-through">
            {formatCurrency(checkoutDraft.price.totalPrice)}
          </p>
        )}
        <span>{formatCurrency(finalPrice)}</span>
      </div>
    </div>
  </div>
</section>
```

### 3.6. Truyền `couponCode` và `totalAmount` chính xác vào payload đặt phòng

Sửa `buildPlaceBookingPayload` trong [`bookingUtils.js`](frontend/src/features/booking/utils/bookingUtils.js) hoặc trực tiếp trong `CheckoutPage.jsx`:

```js
// Trong runPlaceBooking():
const payload = buildPlaceBookingPayload({
  checkoutDraft,
  userId,
  idempotencyKey: idempotencyKeyRef.current,
  forceToken,
  couponCode: appliedCoupon?.code || null,
});

// Trong buildPlaceBookingPayload():
if (couponCode) {
  payload.couponCode = couponCode;
}
// totalAmount trong payload = checkoutDraft.price.totalPrice (giá GỐC)
// promotion-service sẽ tự tính finalAmount trong Saga
payload.totalAmount = Number(checkoutDraft.price.totalPrice);
```

> **Lưu ý:** `totalAmount` gửi lên BE là **giá gốc** (trước giảm), không phải `finalPrice`. BE sẽ tính `finalAmount` trong Saga sau khi validate coupon.

---

## Phần 4 — Thứ tự implement khuyến nghị

1. **[promotion-service]** Tạo DTO cho Kafka commands/events (`dto/kafka/`)
2. **[promotion-service]** Tạo `ValidatePromotionPreRequest/Response`, `CouponDetailResponse` (`dto/request`, `dto/response`)
3. **[promotion-service]** Thêm method `validatePre()` vào `PromotionService` interface + `PromotionServiceImpl`
4. **[promotion-service]** Thêm method `getCouponDetail()` vào `PromotionService` + impl
5. **[promotion-service]** Thêm 2 endpoint mới vào `PromotionController`
6. **[promotion-service]** Tạo `PromotionUsageRepository` với methods cần thiết
7. **[promotion-service]** Tạo `PromotionKafkaConsumerService` với 3 handlers
8. **[promotion-service]** Thêm Kafka consumer config vào `application.yml`
9. **[place-booking-service]** Tạo `PromotionServiceClient` (Feign)
10. **[place-booking-service]** Thêm `couponCode` vào `PlaceBookingRequest`
11. **[place-booking-service]** Thêm `couponCode`, `pendingPayload` vào `SagaState` entity + migration
12. **[place-booking-service]** Tạo các DTO mới (ValidatePromotion, PromotionValidated, ...)
13. **[place-booking-service]** Sửa `PlaceBookingController` — thêm bước pre-validate (Feign call)
14. **[place-booking-service]** Sửa `PlaceBookingService.startSaga()` — phân nhánh có/không coupon, lưu `pendingPayload`
15. **[place-booking-service]** Thêm listener `promotion-events` vào `KafkaConsumerService`
16. **[place-booking-service]** Sửa `handlePaymentSucceeded` để gửi `ConfirmPromotionUsage`
17. **[place-booking-service]** Sửa `handleBookingFailed` và `handlePaymentFailed` để gửi `ReleasePromotionUsage`
18. **[booking-service]** Migration SQL + sửa Entity + sửa handler `CreateBooking`
19. **[frontend]** Tạo `frontend/src/features/promotion/api/promotionApi.js`
20. **[frontend]** Sửa `CheckoutPage.jsx`: thêm coupon section, sửa tổng tiền, truyền `couponCode`

---

## Các ràng buộc kỹ thuật quan trọng

### Naming convention — PHẢI tuân thủ

Theo [`docs/kafka-events.md`](docs/kafka-events.md):
- Topics: `promotion-commands`, `promotion-events`
- Commands: `ValidatePromotion`, `ConfirmPromotionUsage`, `ReleasePromotionUsage`
- Events: `PromotionValidated`, `PromotionRejected`, `PromotionUsageConfirmed`, `PromotionUsageFailed`

### Outbox Pattern — PHẢI dùng

Mọi Kafka event publish phải đi qua `outboxPublisherService.saveOutboxMessage(...)` **trong cùng transaction** với thao tác DB. KHÔNG publish Kafka trực tiếp.

### Race condition — PHẢI xử lý trong `handleValidatePromotion`

Dùng `PESSIMISTIC_WRITE` lock (PostgreSQL `SELECT FOR UPDATE`) khi check và update usage count. Không cần Redis lock (lượt dùng coupon ít hơn booking phòng nhiều).

### Idempotency — PHẢI xử lý

Trường `idempotency_key` trong `promotion_usages` = `sagaId:bookingId`. Kiểm tra trước khi insert:
```java
if (promotionUsageRepository.existsByIdempotencyKey(idempotencyKey)) {
    // Đã xử lý, publish lại PromotionValidated (idempotent response)
    return;
}
```

### Không có FK vật lý giữa các service

`promotion_usages.booking_id` chỉ là UUID tham chiếu, KHÔNG có FK sang `booking_db`.

### SagaState — KHÔNG lưu promotion metadata

`saga_states` chỉ thêm 2 trường:
- `coupon_code` (VARCHAR) — biết saga có dùng coupon không
- `pending_payload` (TEXT/JSON) — snapshot `CreateBooking` command để rebuild sau `PromotionValidated`

**KHÔNG** thêm `promotion_id`, `coupon_id`, `discount_amount`, `final_amount` vào `saga_states`.

---

## Checklist kiểm tra khi hoàn thành

- [ ] Luồng đặt phòng **không có coupon** vẫn hoạt động bình thường (không bị ảnh hưởng)
- [ ] Pre-validate hợp lệ → Saga khởi động, in-saga validate → CreateBooking với finalAmount
- [ ] Pre-validate thất bại → trả 4xx ngay cho FE, **không** khởi động Saga
- [ ] In-saga validate thất bại (race condition) → Saga FAILED, FE nhận thông báo qua FCM
- [ ] Khi payment thất bại sau `PromotionValidated` → `ReleasePromotionUsage` được gửi
- [ ] Khi booking thất bại (hết phòng) sau `PromotionValidated` → `ReleasePromotionUsage` được gửi
- [ ] Không thể dùng cùng 1 coupon 2 lần đồng thời (race condition — DB lock)
- [ ] Idempotency: xử lý trùng Kafka message không tạo 2 usage record
- [ ] API `GET /promotions/coupons/{code}` trả đúng thông tin (không reserve)
- [ ] API `POST /promotions/validate-pre` hoạt động đúng
- [ ] FE: coupon section hiển thị đúng thông tin, giá gốc gạch ngang, giá mới
- [ ] FE: `totalAmount` gửi lên BE là giá GỐC (không phải finalAmount)
- [ ] Bảng `bookings` có cả `original_amount` và `final_amount`

---

## Tham chiếu file code quan trọng

| File | Mô tả |
|------|-------|
| [PlaceBookingController.java](services/place_booking_service/src/main/java/com/place_booking_service/controller/PlaceBookingController.java) | Controller cần thêm Bước pre-validate |
| [KafkaConsumerService.java](services/place_booking_service/src/main/java/com/place_booking_service/service/kafka/KafkaConsumerService.java) | Cần thêm promotionEventsHandler |
| [PlaceBookingService.java](services/place_booking_service/src/main/java/com/place_booking_service/service/PlaceBookingService.java) | startSaga() — cần sửa phân nhánh |
| [SagaState.java](services/place_booking_service/src/main/java/com/place_booking_service/entity/SagaState.java) | Thêm `couponCode`, `pendingPayload` |
| [HotelServiceClient.java](services/place_booking_service/src/main/java/com/place_booking_service/client/HotelServiceClient.java) | Pattern Feign Client để tạo PromotionServiceClient |
| [OutboxPublisherService.java (PBS)](services/place_booking_service/src/main/java/com/place_booking_service/service/OutboxPublisherService.java) | Outbox pattern trong PBS |
| [PromotionController.java](services/promotion-service/src/main/java/com/promotion/promotion_service/controller/PromotionController.java) | Cần thêm 2 endpoint mới |
| [PromotionServiceImpl.java](services/promotion-service/src/main/java/com/promotion/promotion_service/service/impl/PromotionServiceImpl.java) | Cần thêm validatePre(), getCouponDetail() |
| [OutboxPublisherService.java (PS)](services/promotion-service/src/main/java/com/promotion/promotion_service/service/OutboxPublisherService.java) | Outbox pattern trong promotion-service |
| [PromotionUsageEntity.java](services/promotion-service/src/main/java/com/promotion/promotion_service/entity/PromotionUsageEntity.java) | Entity bảng promotion_usages (status RESERVED/CONFIRMED/CANCELLED) |
| [CouponEntity.java](services/promotion-service/src/main/java/com/promotion/promotion_service/entity/CouponEntity.java) | Entity bảng coupons |
| [CouponRepository.java](services/promotion-service/src/main/java/com/promotion/promotion_service/repository/CouponRepository.java) | Cần thêm `findByCodeForUpdate` |
| [CheckoutPage.jsx](frontend/src/pages/CheckoutPage.jsx) | Trang checkout — cần thêm coupon UI |
| [bookingUtils.js](frontend/src/features/booking/utils/bookingUtils.js) | Utility build payload — cần thêm couponCode |
