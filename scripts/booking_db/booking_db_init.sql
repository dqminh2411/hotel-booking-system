-- ============================================================
-- BOOKING SERVICE — Init Script
-- PostgreSQL 18
-- Chạy toàn bộ file này 1 lần trong pgAdmin Query Tool
-- Toàn bộ ID (bookings.id, booked_roomtypes.id, outbox_events.id) đều là UUID
-- Dữ liệu mẫu tham chiếu tới hotel_id/room_type_id có sẵn trong hotel_db.sql
-- (Hanoi Heritage Hotel — 12 Hàng Bạc)
-- ============================================================


-- ────────────────────────────────────────────────────────────
-- EXTENSIONS
-- ────────────────────────────────────────────────────────────
CREATE EXTENSION IF NOT EXISTS "pgcrypto"; -- hỗ trợ gen_random_uuid()


-- ────────────────────────────────────────────────────────────
-- ENUM TYPES
-- ────────────────────────────────────────────────────────────

CREATE TYPE booking_status_enum AS ENUM (
    'FAILED',       -- Không đặt phòng thành công, diễn ra ở bước check availability ở booking service để tránh race-condition
    'PENDING',      -- Phòng đang được giữ chỗ, chờ thanh toán tiền cọc
    'CONFIRMED',    -- Đã thanh toán tiền cọc, booking được xác nhận
    'CANCELLED',    -- Không thanh toán được trong thời hạn, đơn bị huỷ
    'CHECKEDIN',    -- Khách hàng đã check-in
    'COMPLETED'     -- Khách đã thanh toán đầy đủ, check-out, đơn hoàn thành
);

CREATE TYPE payment_method_enum AS ENUM (
    'CREDIT_CARD'       -- Thanh toán qua thẻ tín dụng
);

-- ────────────────────────────────────────────────────────────
-- BẢNG bookings
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS bookings (
    id               UUID                 PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id      UUID                 NOT NULL, -- tham chiếu user-service (không có FK vật lý)
    hotel_id         UUID                 NOT NULL, -- tham chiếu hotel-service (không có FK vật lý)
    created_at       TIMESTAMPTZ          NOT NULL DEFAULT NOW(),
    checkin_date     DATE                 NOT NULL,
    checkout_date    DATE                 NOT NULL,
    num_adults       INTEGER              NOT NULL,
    original_amount  NUMERIC              NOT NULL,
    final_amount     NUMERIC              NOT NULL,
    currency         VARCHAR(255)         NOT NULL DEFAULT 'VND',
    status           booking_status_enum  NOT NULL DEFAULT 'PENDING',
    payment_method   payment_method_enum,
    idempotency_key  VARCHAR(255)         UNIQUE,
    is_deleted       BOOLEAN              NOT NULL DEFAULT false,
    CONSTRAINT chk_bookings_dates           CHECK (checkout_date > checkin_date),
    CONSTRAINT chk_bookings_num_adults      CHECK (num_adults > 0),
    CONSTRAINT chk_bookings_original_amount CHECK (original_amount > 0),
    CONSTRAINT chk_bookings_final_amount    CHECK (final_amount > 0)
);


-- ────────────────────────────────────────────────────────────
-- BẢNG booked_roomtypes
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS booked_roomtypes (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id       UUID          NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    room_type_id     UUID          NOT NULL, -- tham chiếu hotel-service (không có FK vật lý)
    quantity         INTEGER       NOT NULL,
    price_per_night  NUMERIC       NOT NULL,
    nights           INTEGER       NOT NULL,
    subtotal         NUMERIC       NOT NULL,
    room_ids         UUID[],       -- Trường mảng UUID tương ứng với List<UUID> trong Entity
    is_deleted       BOOLEAN       NOT NULL DEFAULT false,
    CONSTRAINT chk_booked_roomtypes_quantity CHECK (quantity > 0),
    CONSTRAINT chk_booked_roomtypes_nights   CHECK (nights > 0)
);


-- ────────────────────────────────────────────────────────────
-- BẢNG booking_info
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS booking_info (
    booking_id      UUID    PRIMARY KEY REFERENCES bookings(id) ON DELETE CASCADE,
    booking_detail  JSONB   NOT NULL,
    is_deleted      BOOLEAN NOT NULL DEFAULT false
);


-- ────────────────────────────────────────────────────────────
-- BẢNG outbox_events
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS outbox_events (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    topic         VARCHAR(100) NOT NULL,
    payload       JSONB       NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at    TIMESTAMP   NOT NULL DEFAULT NOW(),
    published_at  TIMESTAMP,
    retry_count   INTEGER     NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP,
    locked_until  TIMESTAMP,

    CONSTRAINT chk_status CHECK (status IN ('PENDING','PROCESSING','PUBLISHED','DEAD_LETTER'))
    is_deleted    BOOLEAN     NOT NULL DEFAULT false,
    traceparent   VARCHAR(55)
);

CREATE TABLE IF NOT EXISTS roomtype_inventory (
    room_type_id   UUID          PRIMARY KEY,
    hotel_id       UUID          NOT NULL,
    total_quantity INT           NOT NULL,
    synced_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

INSERT INTO roomtype_inventory (room_type_id, hotel_id, total_quantity) VALUES
    -- Standard Room (Số lượng gốc: 10)
    ('d1000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 10),
    
    -- Deluxe Room (Số lượng gốc: 8)
    ('d1000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000001', 8),
    
    -- Superior Twin Room (Số lượng gốc: 6)
    ('d1000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000001', 6),
    
    -- Junior Suite (Số lượng gốc: 4)
    ('d1000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000001', 4),
    
    -- Presidential Suite (Số lượng gốc: 1) -> Rất hợp lý để test giành giật phòng hiếm!
    ('d1000000-0000-0000-0000-000000000005', 'b0000000-0000-0000-0000-000000000001', 2)
ON CONFLICT (room_type_id) DO NOTHING;


-- ────────────────────────────────────────────────────────────
-- INDEXES
-- ────────────────────────────────────────────────────────────

-- Dùng cho availability check (hotel-service gọi GET /bookings/count)
-- Query: WHERE room_type_id = ? AND status IN ('PENDING','CONFIRMED')
--        AND checkin_date < :checkout AND checkout_date > :checkin
CREATE INDEX IF NOT EXISTS idx_bookings_availability
    ON bookings (checkin_date, checkout_date, status);

CREATE INDEX IF NOT EXISTS idx_booked_roomtypes_room_type
    ON booked_roomtypes (room_type_id);

CREATE INDEX IF NOT EXISTS idx_booked_roomtypes_booking
    ON booked_roomtypes (booking_id);

CREATE INDEX IF NOT EXISTS idx_bookings_customer
    ON bookings (customer_id);

CREATE INDEX IF NOT EXISTS idx_bookings_status
    ON bookings (status);

-- Index quan trọng cho OutboxRelay: chỉ scan các event chưa publish
CREATE INDEX IF NOT EXISTS idx_outbox_unpublished
    ON outbox_events (published, created_at)
    WHERE published = false;


-- ────────────────────────────────────────────────────────────
-- VERIFY — kiểm tra nhanh sau khi chạy
-- ────────────────────────────────────────────────────────────
SELECT 'bookings'          AS tbl, COUNT(*) FROM bookings
UNION ALL
SELECT 'booked_roomtypes',         COUNT(*) FROM booked_roomtypes
UNION ALL
SELECT 'booking_info',             COUNT(*) FROM booking_info
UNION ALL
SELECT 'outbox_events',            COUNT(*) FROM outbox_events;