# Kafka Events

## 1. Kafka Topics

> **Chú thích:** các dòng/ô có nhãn **(MỚI)** là phần được bổ sung trong bản cập nhật này so với file `kafka-topic-events.md` gốc. Phần còn lại giữ nguyên không đổi.

| Topic | Publisher | Consumer | Event Types |
|-------|-----------|----------|-------------|
| `booking-commands` | place-booking-service | booking-service | `CreateBooking`, `ConfirmBooking`, `CancelBooking` |
| `booking-events` | booking-service | place-booking-service | `BookingCreated`, `BookingConfirmed`, `BookingCancelled`, `BookingFailed` |
| `payment-commands` | place-booking-service | payment-service | `ProcessPayment`, `RefundPayment` |
| `payment-events` | payment-service | place-booking-service | `PaymentSucceeded`, `PaymentFailed`, `PaymentRefunded`, **`RefundFailed` (MỚI)** |
| **`promotion-commands` (MỚI)** | **place-booking-service** | **promotion-service** | **`ValidatePromotion`, `ConfirmPromotionUsage`, `ReleasePromotionUsage`** |
| **`promotion-events` (MỚI)** | **promotion-service** | **place-booking-service** | **`PromotionValidated`, `PromotionRejected`, `PromotionUsageConfirmed`, `PromotionUsageFailed`** |
| **`promotion-active-notification` (MỚI)** | **promotion-service** | **notification-service** | **`PromotionActiveCreated`, `PromotionActiveUpdated`** |
| **`coupon-active-notification` (MỚI)** | **promotion-service** | **notification-service** | **`CouponActiveCreated`, `CouponActiveUpdated`** |
| `notification-commands` | place-booking-service | notification-service | `SendBookingConfirmed`, `SendBookingFailed`, **`SendBookingCancelled` (MỚI)**, **`SendRefundFailed` (MỚI)** |

## 2. Event Schema

**Common object schemas**

`User`

| Attribute | Type | Description |
|-----------|------|-------------|
| `userId` | `String` | ID của người dùng. |
| `name` | `String` | Tên hiển thị của người dùng. |
| `email` | `String` | Email nhận thông báo/liên hệ. |

`Hotel`

| Attribute | Type | Description |
|-----------|------|-------------|
| `hotelId` | `String` | ID khách sạn. |
| `name` | `String` | Tên khách sạn. |
| `address` | `String` | Địa chỉ khách sạn. |

`RoomType`

| Attribute | Type | Description |
|-----------|------|-------------|
| `roomTypeId` | `String` | ID loại phòng được đặt. |
| `name` | `String` | Tên loại phòng. |
| `bedCount` | `int` | Số giường của loại phòng. |
| `bookingQuantity` | `int` | Số lượng phòng khách đặt. |
| `totalQuantity` | `int` | Tổng số phòng của loại phòng. |
| `price` | `Double/BigDecimal` | Giá phòng dùng để tính tổng tiền. |

`BookingInfo`

| Attribute | Type | Description |
|-----------|------|-------------|
| `bookingId` | `String` | ID booking. |
| `customer` | `User` | Thông tin khách hàng đặt phòng. |
| `checkin` | `String` | Ngày nhận phòng. |
| `checkout` | `String` | Ngày trả phòng. |
| `numAdults` | `int/Integer` | Số người lớn. |
| `totalAmount` | `Double/BigDecimal` | Tổng số tiền booking. |
| `hotel` | `Hotel` | Thông tin khách sạn. |
| `roomTypeList` | `List<RoomType>` | Danh sách loại phòng và số lượng đặt. |

**`booking-commands`**

| Event Type | Attribute | Type | Description |
|------------|-----------|------|-------------|
| `CreateBooking` | `sagaId` | `String` | ID của saga đặt phòng. |
|  | `eventType` | `String` | Giá trị `CreateBooking`. |
|  | `user` | `User` | Khách hàng đặt phòng. |
|  | `hotel` | `Hotel` | Khách sạn được đặt. |
|  | `bookingId` | `String` | ID booking do orchestrator tạo. |
|  | `roomTypeList` | `List<RoomType>` | Danh sách loại phòng cần tạo booking. |
|  | `checkin` | `String/LocalDate` | Ngày nhận phòng. |
|  | `checkout` | `String/LocalDate` | Ngày trả phòng. |
|  | `numAdults` | `int/Integer` | Số người lớn. |
|  | `totalAmount` | `Double/BigDecimal` | Tổng tiền cần thanh toán. |
|  | `currency` | `String` | Mã tiền tệ, ví dụ `VND`. |
|  | `paymentMethod` | `String/PaymentMethod` | Phương thức thanh toán. |
|  | `paymentToken` | `String` | Token/tham chiếu thanh toán từ client. |
| `ConfirmBooking` | `sagaId` | `String` | ID của saga đặt phòng. |
|  | `eventType` | `String` | Giá trị `ConfirmBooking`. |
|  | `bookingId` | `String` | Booking cần chuyển sang CONFIRMED. |
| `CancelBooking` | `sagaId` | `String` | ID của saga đặt phòng. |
|  | `eventType` | `String` | Giá trị `CancelBooking`. |
|  | `bookingId` | `String` | Booking cần hủy. |
|  | `reason` | `String` | Lý do hủy booking. |

**`booking-events`**

| Event Type | Attribute | Type | Description |
|------------|-----------|------|-------------|
| `BookingCreated` | `sagaId` | `String` | ID của saga đặt phòng. |
|  | `eventType` | `String` | Giá trị `BookingCreated`. |
|  | `bookingId` | `String` | Booking đã được tạo ở trạng thái PENDING. |
|  | `userId` | `String` | ID khách hàng đặt phòng. |
|  | `totalAmount` | `Double/BigDecimal` | Tổng tiền cần thanh toán. |
|  | `currency` | `String` | Mã tiền tệ. |
|  | `paymentMethod` | `String/PaymentMethod` | Phương thức thanh toán. |
|  | `paymentToken` | `String` | Token/tham chiếu thanh toán. |
| `BookingConfirmed` | `sagaId` | `String` | ID của saga đặt phòng. |
|  | `eventType` | `String` | Giá trị `BookingConfirmed`. |
|  | `booking` | `BookingInfo` | Chi tiết booking đã xác nhận. |
| `BookingCancelled` | `sagaId` | `String` | ID của saga đặt phòng. |
|  | `eventType` | `String` | Giá trị `BookingCancelled`. |
|  | `booking` | `BookingInfo` | Chi tiết booking đã hủy. |
|  | `reason` | `String` | Lý do hủy booking. |
| `BookingFailed` | `sagaId` | `String` | ID của saga đặt phòng. |
|  | `eventType` | `String` | Giá trị `BookingFailed`. |
|  | `booking` | `BookingInfo` | Chi tiết booking thất bại. |
|  | `reason` | `String` | Lý do tạo booking thất bại. |

**`payment-commands`**

| Event Type | Attribute | Type | Description |
|------------|-----------|------|-------------|
| `ProcessPayment` | `eventType` | `String` | Giá trị `ProcessPayment`. |
|  | `sagaId` | `String` | ID của saga đặt phòng. |
|  | `bookingId` | `String` | Booking cần thanh toán. |
|  | `amount` | `Double` | Số tiền cần thu. |
|  | `currency` | `String` | Mã tiền tệ. |
|  | `paymentMethod` | `String` | Phương thức thanh toán. |
|  | `paymentToken` | `String` | Token/tham chiếu thanh toán. |
|  | `idempotencyKey` | `String` | Khóa chống xử lý trùng thanh toán. |
|  | `userId` | `String` | ID khách hàng thanh toán. |
| `RefundPayment` | `eventType` | `String` | Giá trị `RefundPayment`. |
|  | `sagaId` | `String` | ID của saga đặt phòng. |
|  | `bookingId` | `String` | Booking cần hoàn tiền. |
|  | `paymentId` | `String` | Giao dịch thanh toán cần hoàn. |
|  | **`refundAmount`** | **`Double`** | **Số tiền cần hoàn — do place-booking-service tính theo chính sách hủy (miễn phí/có phí/không hoàn). (MỚI)** |
|  | **`currency`** | **`String`** | **Mã tiền tệ. (MỚI)** |
|  | `reason` | `String` | Lý do hoàn tiền. |

**`payment-events`**

| Event Type | Attribute | Type | Description |
|------------|-----------|------|-------------|
| `PaymentSucceeded` | `eventType` | `String` | Giá trị `PaymentSucceeded`. |
|  | `sagaId` | `String` | ID của saga đặt phòng. |
|  | `bookingId` | `String` | Booking đã thanh toán thành công. |
|  | `paymentId` | `String` | ID giao dịch thanh toán. |
|  | `amount` | `Double` | Số tiền đã thu. |
|  | `currency` | `String` | Mã tiền tệ. |
|  | `transactionRef` | `String` | Mã tham chiếu từ payment gateway. |
|  | `processedAt` | `String` | Thời điểm xử lý thanh toán. |
| `PaymentFailed` | `eventType` | `String` | Giá trị `PaymentFailed`. |
|  | `sagaId` | `String` | ID của saga đặt phòng. |
|  | `bookingId` | `String` | Booking thanh toán thất bại. |
|  | `reason` | `String` | Lý do thanh toán thất bại. |
| `PaymentRefunded` | `eventType` | `String` | Giá trị `PaymentRefunded`. |
|  | `sagaId` | `String` | ID của saga đặt phòng. |
|  | `bookingId` | `String` | Booking đã được hoàn tiền. |
|  | `paymentId` | `String` | Giao dịch thanh toán đã hoàn. |
|  | `amount` | `Double` | Số tiền đã hoàn. |
|  | `currency` | `String` | Mã tiền tệ. |
|  | `refundedAt` | `LocalDateTime` | Thời điểm hoàn tiền. |
| **`RefundFailed` (MỚI)** | **`eventType`** | **`String`** | **Giá trị `RefundFailed`. (MỚI)** |
|  | **`sagaId`** | **`String`** | **ID của saga hủy booking. (MỚI)** |
|  | **`bookingId`** | **`String`** | **Booking hoàn tiền thất bại. (MỚI)** |
|  | **`paymentId`** | **`String`** | **Giao dịch thanh toán cần hoàn nhưng thất bại. (MỚI)** |
|  | **`reason`** | **`String`** | **Lý do hoàn tiền thất bại (gateway từ chối / CircuitBreaker OPEN). (MỚI)** |

**`notification-commands`**

| Event Type | Attribute | Type | Description |
|------------|-----------|------|-------------|
| `SendBookingConfirmed` | `eventType` | `String` | Giá trị `SendBookingConfirmed`. |
|  | `sagaId` | `String` | ID của saga đặt phòng. |
|  | `bookingId` | `String` | Booking cần gửi thông báo xác nhận. |
|  | `to` | `String` | Email người nhận. |
|  | `booking` | `BookingInfo` | Chi tiết booking để render email. |
| `SendBookingFailed` | `eventType` | `String` | Giá trị `SendBookingFailed`. |
|  | `sagaId` | `String` | ID của saga đặt phòng. |
|  | `bookingId` | `String` | Booking cần gửi thông báo thất bại/hủy. |
|  | `to` | `String` | Email người nhận. |
|  | `booking` | `BookingInfo` | Chi tiết booking để render email. |
|  | `reason` | `String` | Lý do thất bại hoặc hủy booking. |
| **`SendBookingCancelled` (MỚI)** | **`eventType`** | **`String`** | **Giá trị `SendBookingCancelled`. (MỚI)** |
|  | **`sagaId`** | **`String`** | **ID của saga hủy booking. (MỚI)** |
|  | **`bookingId`** | **`String`** | **Booking đã hủy cần gửi thông báo. (MỚI)** |
|  | **`to`** | **`String`** | **Email người nhận. (MỚI)** |
|  | **`booking`** | **`BookingInfo`** | **Chi tiết booking để render email. (MỚI)** |
|  | **`refunded`** | **`boolean`** | **Có hoàn tiền hay không theo chính sách hủy. (MỚI)** |
|  | **`refundAmount`** | **`Double`** | **Số tiền đã/được hoàn (`null` nếu chính sách "không hoàn"). (MỚI)** |
| **`SendRefundFailed` (MỚI)** | **`eventType`** | **`String`** | **Giá trị `SendRefundFailed`. (MỚI)** |
|  | **`sagaId`** | **`String`** | **ID của saga hủy booking. (MỚI)** |
|  | **`bookingId`** | **`String`** | **Booking liên quan đến hoàn tiền thất bại. (MỚI)** |
|  | **`to`** | **`String`** | **Email người nhận (khách hàng). (MỚI)** |
|  | **`booking`** | **`BookingInfo`** | **Chi tiết booking để render email. (MỚI)** |
|  | **`reason`** | **`String`** | **Lý do hoàn tiền thất bại. (MỚI)** |

**`promotion-commands`**

| Event Type | Attribute | Type | Description |
|------------|-----------|------|-------------|
| `ValidatePromotion` | `eventType` | `String` | Giá trị `ValidatePromotion`. |
|  | `sagaId` | `String` | ID của saga đặt phòng. |
|  | `bookingId` | `String` | ID booking đang được xử lý. |
|  | `userId` | `String` | ID khách hàng áp dụng promotion. |
|  | `couponCode` | `String` | Mã coupon cần kiểm tra. |
|  | `hotelId` | `String` | ID khách sạn được đặt. |
|  | `roomTypeIds` | `List<String>` | Danh sách loại phòng trong booking. |
|  | `checkin` | `LocalDate/String` | Ngày nhận phòng. |
|  | `checkout` | `LocalDate/String` | Ngày trả phòng. |
|  | `totalAmount` | `BigDecimal/Double` | Tổng tiền trước giảm giá. |
| `ConfirmPromotionUsage` | `eventType` | `String` | Giá trị `ConfirmPromotionUsage`. |
|  | `sagaId` | `String` | ID của saga đặt phòng. |
|  | `bookingId` | `String` | Booking đã thanh toán thành công. |
|  | `promotionId` | `String` | Promotion đã được áp dụng. |
|  | `couponId` | `String` | Coupon đã được áp dụng. |
|  | `userId` | `String` | Người dùng promotion. |
|  | `discountAmount` | `BigDecimal/Double` | Số tiền được giảm. |
| `ReleasePromotionUsage` | `eventType` | `String` | Giá trị `ReleasePromotionUsage`. |
|  | `sagaId` | `String` | ID của saga cần bù trừ. |
|  | `bookingId` | `String` | Booking thất bại hoặc bị hủy. |
|  | `promotionId` | `String` | Promotion cần giải phóng lượt giữ. |
|  | `couponId` | `String` | Coupon cần giải phóng lượt giữ. |
|  | `reason` | `String` | Lý do thực hiện bù trừ. |

**`promotion-events`**

| Event Type | Attribute | Type | Description |
|------------|-----------|------|-------------|
| `PromotionValidated` | `eventType` | `String` | Giá trị `PromotionValidated`. |
|  | `sagaId` | `String` | ID của saga đặt phòng. |
|  | `bookingId` | `String` | Booking được áp dụng promotion. |
|  | `promotionId` | `String` | Promotion hợp lệ. |
|  | `couponId` | `String` | Coupon hợp lệ. |
|  | `discountAmount` | `BigDecimal/Double` | Số tiền được giảm. |
|  | `finalAmount` | `BigDecimal/Double` | Tổng tiền sau giảm giá. |
| `PromotionRejected` | `eventType` | `String` | Giá trị `PromotionRejected`. |
|  | `sagaId` | `String` | ID của saga đặt phòng. |
|  | `bookingId` | `String` | Booking không áp dụng được promotion. |
|  | `reason` | `String` | Lý do promotion bị từ chối. |
| `PromotionUsageConfirmed` | `eventType` | `String` | Giá trị `PromotionUsageConfirmed`. |
|  | `sagaId` | `String` | ID của saga đặt phòng. |
|  | `bookingId` | `String` | Booking đã ghi nhận sử dụng promotion. |
|  | `promotionId` | `String` | Promotion đã tăng usage count. |
|  | `couponId` | `String` | Coupon đã tăng usage count. |
| `PromotionUsageFailed` | `eventType` | `String` | Giá trị `PromotionUsageFailed`. |
|  | `sagaId` | `String` | ID của saga đặt phòng. |
|  | `bookingId` | `String` | Booking không ghi nhận được usage. |
|  | `reason` | `String` | Lý do xác nhận usage thất bại. |

**`promotion-active-notification` (MỚI)**

| Event Type | Attribute | Type | Description |
|------------|-----------|------|-------------|
| `PromotionActiveCreated` / `PromotionActiveUpdated` | `eventId` | `UUID` | ID của sự kiện outbox. |
|  | `eventType` | `String` | Giá trị `PromotionActiveCreated` hoặc `PromotionActiveUpdated`. |
|  | `promotionId` | `UUID` | ID khuyến mãi. |
|  | `name` | `String` | Tên chương trình khuyến mãi. |
|  | `description` | `String` | Mô tả chi tiết khuyến mãi. |
|  | `discountType` | `String` | Loaị giảm giá (`PERCENTAGE` / `FIXED_AMOUNT`). |
|  | `discountValue` | `BigDecimal` | Giá trị giảm. |
|  | `maxDiscountAmount` | `BigDecimal` | Số tiền giảm tối đa. |
|  | `startAt` | `OffsetDateTime` | Thời gian bắt đầu hiệu lực. |
|  | `endAt` | `OffsetDateTime` | Thời gian kết thúc hiệu lực. |
|  | `scopeType` | `String` | Phạm vi áp dụng (ví dụ: `SYSTEM`, `HOTEL`). |
|  | `occurredAt` | `OffsetDateTime` | Thời điểm phát sinh sự kiện. |

**`coupon-active-notification` (MỚI)**

| Event Type | Attribute | Type | Description |
|------------|-----------|------|-------------|
| `CouponActiveCreated` / `CouponActiveUpdated` | `eventId` | `UUID` | ID của sự kiện outbox. |
|  | `eventType` | `String` | Giá trị `CouponActiveCreated` hoặc `CouponActiveUpdated`. |
|  | `couponId` | `UUID` | ID coupon. |
|  | `promotionId` | `UUID` | ID promotion chứa coupon. |
|  | `code` | `String` | Mã giảm giá coupon (ví dụ: `SUMMER2026`). |
|  | `promotionName` | `String` | Tên chương trình khuyến mãi đi kèm. |
|  | `discountType` | `String` | Loại giảm giá (`PERCENTAGE` / `FIXED_AMOUNT`). |
|  | `discountValue` | `BigDecimal` | Giá trị giảm. |
|  | `usageLimit` | `Integer` | Giới hạn số lượt dùng. |
|  | `occurredAt` | `OffsetDateTime` | Thời điểm phát sinh sự kiện. |

