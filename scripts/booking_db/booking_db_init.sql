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
    total_amount     NUMERIC              NOT NULL,
    currency         VARCHAR(255)         NOT NULL DEFAULT 'VND',
    status           booking_status_enum  NOT NULL DEFAULT 'PENDING',
    payment_method   payment_method_enum,
    idempotency_key  VARCHAR(255)         UNIQUE,
    is_deleted       BOOLEAN              NOT NULL DEFAULT false,
    CONSTRAINT chk_bookings_dates      CHECK (checkout_date > checkin_date),
    CONSTRAINT chk_bookings_num_adults CHECK (num_adults > 0),
    CONSTRAINT chk_bookings_amount     CHECK (total_amount > 0)
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
    published     BOOLEAN     NOT NULL DEFAULT false,
    created_at    TIMESTAMP   NOT NULL DEFAULT NOW(),
    published_at  TIMESTAMP,
    is_deleted    BOOLEAN     NOT NULL DEFAULT false
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
    ('d1000000-0000-0000-0000-000000000005', 'b0000000-0000-0000-0000-000000000001', 1)
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


-- ============================================================
-- SEED DATA
-- Khách sạn: Hanoi Heritage Hotel  → hotel_id = b0000000-0000-0000-0000-000000000001
-- Room types tham chiếu từ hotel_db.sql:
--   d1000000-...-0001  Standard Room       1.200.000đ/đêm
--   d1000000-...-0002  Deluxe Room         1.800.000đ/đêm
--   d1000000-...-0003  Superior Twin Room  2.000.000đ/đêm
--   d1000000-...-0004  Junior Suite        3.500.000đ/đêm
--   d1000000-...-0005  Presidential Suite  8.500.000đ/đêm
-- customer_id là UUID giả lập (tham chiếu logic tới user-service, chưa có bảng users ở đây)
-- ============================================================

-- ────────────────────────────────────────────────────────────
-- 1. Bookings — phủ đủ 6 trạng thái của booking_status_enum
-- ────────────────────────────────────────────────────────────
INSERT INTO bookings
    (id, customer_id, hotel_id, checkin_date, checkout_date, num_adults,
     total_amount, currency, status, payment_method, idempotency_key)
VALUES
    -- PENDING: vừa giữ chỗ, chưa thanh toán cọc
    ('e1000000-0000-0000-0000-000000000001',
     'c1000000-0000-0000-0000-000000000001',
     'b0000000-0000-0000-0000-000000000001',
     '2026-07-10', '2026-07-12', 2,
     2400000, 'VND', 'PENDING', NULL,
     'idem-key-0001'),

    -- CONFIRMED: đã thanh toán cọc bằng thẻ tín dụng
    ('e1000000-0000-0000-0000-000000000002',
     'c1000000-0000-0000-0000-000000000002',
     'b0000000-0000-0000-0000-000000000001',
     '2026-07-15', '2026-07-18', 2,
     5400000, 'VND', 'CONFIRMED', 'CREDIT_CARD',
     'idem-key-0002'),

    -- CHECKEDIN: khách đã nhận phòng
    ('e1000000-0000-0000-0000-000000000003',
     'c1000000-0000-0000-0000-000000000003',
     'b0000000-0000-0000-0000-000000000001',
     '2026-07-01', '2026-07-04', 3,
     10500000, 'VND', 'CHECKEDIN', 'CREDIT_CARD',
     'idem-key-0003'),

    -- COMPLETED: booking gồm 2 loại phòng, khách đã check-out
    ('e1000000-0000-0000-0000-000000000004',
     'c1000000-0000-0000-0000-000000000001',
     'b0000000-0000-0000-0000-000000000001',
     '2026-06-20', '2026-06-23', 4,
     31500000, 'VND', 'COMPLETED', 'CREDIT_CARD',
     'idem-key-0004'),

    -- CANCELLED: khách tự huỷ trước hạn thanh toán
    ('e1000000-0000-0000-0000-000000000005',
     'c1000000-0000-0000-0000-000000000002',
     'b0000000-0000-0000-0000-000000000001',
     '2026-08-01', '2026-08-03', 2,
     4800000, 'VND', 'CANCELLED', NULL,
     'idem-key-0005'),

    -- FAILED: check availability thất bại do race-condition, không giữ được phòng
    ('e1000000-0000-0000-0000-000000000006',
     'c1000000-0000-0000-0000-000000000003',
     'b0000000-0000-0000-0000-000000000001',
     '2026-08-10', '2026-08-12', 1,
     3600000, 'VND', 'FAILED', NULL,
     'idem-key-0006');


-- ────────────────────────────────────────────────────────────
-- 2. Booked room types — chi tiết loại phòng theo từng booking
-- ────────────────────────────────────────────────────────────
INSERT INTO booked_roomtypes
    (id, booking_id, room_type_id, quantity, price_per_night, nights, subtotal)
VALUES
    -- Booking 1 (PENDING) — Standard Room, 2 đêm
    ('f1000000-0000-0000-0000-000000000001',
     'e1000000-0000-0000-0000-000000000001',
     'd1000000-0000-0000-0000-000000000001',
     1, 1200000, 2, 2400000),

    -- Booking 2 (CONFIRMED) — Deluxe Room, 3 đêm
    ('f1000000-0000-0000-0000-000000000002',
     'e1000000-0000-0000-0000-000000000002',
     'd1000000-0000-0000-0000-000000000002',
     1, 1800000, 3, 5400000),

    -- Booking 3 (CHECKEDIN) — Junior Suite, 3 đêm
    ('f1000000-0000-0000-0000-000000000003',
     'e1000000-0000-0000-0000-000000000003',
     'd1000000-0000-0000-0000-000000000004',
     1, 3500000, 3, 10500000),

    -- Booking 4 (COMPLETED) — Superior Twin Room + Presidential Suite, 3 đêm
    ('f1000000-0000-0000-0000-000000000004',
     'e1000000-0000-0000-0000-000000000004',
     'd1000000-0000-0000-0000-000000000003',
     1, 2000000, 3, 6000000),
    ('f1000000-0000-0000-0000-000000000005',
     'e1000000-0000-0000-0000-000000000004',
     'd1000000-0000-0000-0000-000000000005',
     1, 8500000, 3, 25500000),

    -- Booking 5 (CANCELLED) — Standard Room x2, 2 đêm
    ('f1000000-0000-0000-0000-000000000006',
     'e1000000-0000-0000-0000-000000000005',
     'd1000000-0000-0000-0000-000000000001',
     2, 1200000, 2, 4800000),

    -- Booking 6 (FAILED) — Deluxe Room, 2 đêm (không giữ được phòng)
    ('f1000000-0000-0000-0000-000000000007',
     'e1000000-0000-0000-0000-000000000006',
     'd1000000-0000-0000-0000-000000000002',
     1, 1800000, 2, 3600000);


-- ────────────────────────────────────────────────────────────
-- 3. Booking info — snapshot chi tiết tại thời điểm đặt phòng
-- ────────────────────────────────────────────────────────────
INSERT INTO booking_info (booking_id, booking_detail) VALUES
    ('e1000000-0000-0000-0000-000000000001', '{
        "hotelName": "Hanoi Heritage Hotel",
        "address": "12 Hàng Bạc, Hoàn Kiếm, Hà Nội",
        "roomTypes": [{"name": "Standard Room", "quantity": 1, "pricePerNight": 1200000}],
        "customerName": "Nguyễn Văn A",
        "customerPhone": "0901000001"
    }'),
    ('e1000000-0000-0000-0000-000000000002', '{
        "hotelName": "Hanoi Heritage Hotel",
        "address": "12 Hàng Bạc, Hoàn Kiếm, Hà Nội",
        "roomTypes": [{"name": "Deluxe Room", "quantity": 1, "pricePerNight": 1800000}],
        "customerName": "Trần Thị B",
        "customerPhone": "0901000002"
    }'),
    ('e1000000-0000-0000-0000-000000000003', '{
        "hotelName": "Hanoi Heritage Hotel",
        "address": "12 Hàng Bạc, Hoàn Kiếm, Hà Nội",
        "roomTypes": [{"name": "Junior Suite", "quantity": 1, "pricePerNight": 3500000}],
        "customerName": "Lê Văn C",
        "customerPhone": "0901000003"
    }'),
    ('e1000000-0000-0000-0000-000000000004', '{
        "hotelName": "Hanoi Heritage Hotel",
        "address": "12 Hàng Bạc, Hoàn Kiếm, Hà Nội",
        "roomTypes": [
            {"name": "Superior Twin Room", "quantity": 1, "pricePerNight": 2000000},
            {"name": "Presidential Suite", "quantity": 1, "pricePerNight": 8500000}
        ],
        "customerName": "Nguyễn Văn A",
        "customerPhone": "0901000001"
    }'),
    ('e1000000-0000-0000-0000-000000000005', '{
        "hotelName": "Hanoi Heritage Hotel",
        "address": "12 Hàng Bạc, Hoàn Kiếm, Hà Nội",
        "roomTypes": [{"name": "Standard Room", "quantity": 2, "pricePerNight": 1200000}],
        "customerName": "Trần Thị B",
        "customerPhone": "0901000002",
        "cancellationReason": "Khách tự huỷ, không thanh toán trong thời hạn"
    }'),
    ('e1000000-0000-0000-0000-000000000006', '{
        "hotelName": "Hanoi Heritage Hotel",
        "address": "12 Hàng Bạc, Hoàn Kiếm, Hà Nội",
        "roomTypes": [{"name": "Deluxe Room", "quantity": 1, "pricePerNight": 1800000}],
        "customerName": "Lê Văn C",
        "customerPhone": "0901000003",
        "failReason": "Hết phòng trống tại thời điểm xác nhận (race-condition)"
    }');


-- ────────────────────────────────────────────────────────────
-- 4. Outbox events — sự kiện phát sinh tương ứng với các booking trên
-- ────────────────────────────────────────────────────────────
INSERT INTO outbox_events (topic, payload, published, published_at) VALUES
    ('booking-events',
     '{"eventType": "BookingCreated", "bookingId": "e1000000-0000-0000-0000-000000000001", "status": "PENDING"}',
     true, NOW() - INTERVAL '2 day'),

    ('booking-events',
     '{"eventType": "BookingConfirmed", "bookingId": "e1000000-0000-0000-0000-000000000002", "status": "CONFIRMED"}',
     true, NOW() - INTERVAL '1 day'),

    ('booking-events',
     '{"eventType": "BookingCheckedIn", "bookingId": "e1000000-0000-0000-0000-000000000003", "status": "CHECKEDIN"}',
     true, NOW() - INTERVAL '12 hour'),

    ('booking-events',
     '{"eventType": "BookingCompleted", "bookingId": "e1000000-0000-0000-0000-000000000004", "status": "COMPLETED"}',
     true, NOW() - INTERVAL '6 hour'),

    ('booking-events',
     '{"eventType": "BookingCancelled", "bookingId": "e1000000-0000-0000-0000-000000000005", "status": "CANCELLED"}',
     true, NOW() - INTERVAL '3 hour'),

    ('booking-events',
     '{"eventType": "BookingFailed", "bookingId": "e1000000-0000-0000-0000-000000000006", "status": "FAILED"}',
     false, NULL);


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