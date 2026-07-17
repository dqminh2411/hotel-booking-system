-- ============================================================
-- HOTEL SERVICE — Init Script
-- PostgreSQL 18
-- Chạy toàn bộ file này 1 lần trong pgAdmin Query Tool
-- ============================================================


-- ────────────────────────────────────────────────────────────
-- EXTENSIONS
-- ────────────────────────────────────────────────────────────
CREATE EXTENSION IF NOT EXISTS "pgcrypto"; -- hỗ trợ gen_random_uuid()


-- ────────────────────────────────────────────────────────────
-- ENUM TYPES
-- ────────────────────────────────────────────────────────────
CREATE TYPE hotel_status_enum   AS ENUM ('PENDING', 'APPROVED', 'SUSPENDED');
CREATE TYPE scope_type_enum     AS ENUM ('HOTEL', 'ROOM_TYPE', 'BOTH');
CREATE TYPE room_status_enum    AS ENUM ('AVAILABLE', 'OCCUPIED', 'CLEANING', 'MAINTENANCE');
CREATE TYPE pricing_type_enum   AS ENUM ('SEASONAL', 'WEEKEND', 'SPECIAL');
CREATE TYPE adjustment_type_enum AS ENUM ('FIXED_PRICE', 'PERCENTAGE_INCREASE', 'AMOUNT_INCREASE');
CREATE TYPE policy_type_enum    AS ENUM ('CANCELATION', 'CHECKIN', 'CHECKOUT', 'SMOKING', 'PAYMENT', 'PETS', 'CHILDREN');


-- ────────────────────────────────────────────────────────────
-- BẢNG ĐỊA LÝ (provinces → districts → wards)
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS provinces (
    code       VARCHAR(2)   PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    name_en    VARCHAR(100),
    full_name  VARCHAR(255),
    is_deleted BOOLEAN      NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS districts (
    code          VARCHAR(3)  PRIMARY KEY,
    province_code VARCHAR(2)  NOT NULL REFERENCES provinces(code),
    name          VARCHAR(100) NOT NULL,
    name_en       VARCHAR(100),
    full_name     VARCHAR(255),
    is_deleted    BOOLEAN      NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS wards (
    code          VARCHAR(5)   PRIMARY KEY,
    district_code VARCHAR(3)   NOT NULL REFERENCES districts(code),
    name          VARCHAR(100) NOT NULL,
    name_en       VARCHAR(100),
    full_name     VARCHAR(255),
    is_deleted    BOOLEAN      NOT NULL DEFAULT false
);


-- ────────────────────────────────────────────────────────────
-- BẢNG KHÁCH SẠN
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS hotels (
    id            UUID                PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID                NOT NULL,
    name          VARCHAR(255)        NOT NULL,
    description   TEXT,
    address       VARCHAR(500)        NOT NULL,
    province_code VARCHAR(2)          NOT NULL REFERENCES provinces(code),
    district_code VARCHAR(3)          NOT NULL REFERENCES districts(code),
    ward_code     VARCHAR(5)          NOT NULL REFERENCES wards(code),
    status        hotel_status_enum   NOT NULL DEFAULT 'PENDING',
    is_deleted    BOOLEAN             NOT NULL DEFAULT false,
    created_at    TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ         NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS hotel_images (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    hotel_id   UUID         NOT NULL REFERENCES hotels(id) ON DELETE CASCADE,
    url        VARCHAR(500) NOT NULL,
    is_cover   BOOLEAN      NOT NULL DEFAULT false,
    is_deleted BOOLEAN      NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS policies (
    id          UUID              PRIMARY KEY DEFAULT gen_random_uuid(),
    hotel_id    UUID              NOT NULL REFERENCES hotels(id) ON DELETE CASCADE,
    type        policy_type_enum  NOT NULL,
    description TEXT,
    is_deleted  BOOLEAN           NOT NULL DEFAULT false
);


-- ────────────────────────────────────────────────────────────
-- BẢNG TIỆN ÍCH
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS amenities (
    id         UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(100)    NOT NULL UNIQUE,
    scope      scope_type_enum NOT NULL,
    is_deleted BOOLEAN         NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS hotel_amenities (
    hotel_id   UUID    NOT NULL REFERENCES hotels(id)    ON DELETE CASCADE,
    amenity_id UUID    NOT NULL REFERENCES amenities(id) ON DELETE CASCADE,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    PRIMARY KEY (hotel_id, amenity_id)
);


-- ────────────────────────────────────────────────────────────
-- BẢNG LOẠI PHÒNG
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS room_types (
    id                   UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    hotel_id             UUID          NOT NULL REFERENCES hotels(id) ON DELETE CASCADE,
    name                 VARCHAR(255)  NOT NULL,
    description          TEXT,
    base_price_per_night DECIMAL(12,2) NOT NULL,
    max_guests           SMALLINT      NOT NULL,
    area                 INT,
    bed_counts           INT           NOT NULL,
    quantity             INT           NOT NULL,
    is_deleted           BOOLEAN       NOT NULL DEFAULT false,
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS room_type_images (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    room_type_id UUID         NOT NULL REFERENCES room_types(id) ON DELETE CASCADE,
    url          VARCHAR(500) NOT NULL,
    is_cover     BOOLEAN      NOT NULL DEFAULT false,
    is_deleted   BOOLEAN      NOT NULL DEFAULT false,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS room_type_amenities (
    room_type_id UUID    NOT NULL REFERENCES room_types(id)  ON DELETE CASCADE,
    amenity_id   UUID    NOT NULL REFERENCES amenities(id)   ON DELETE CASCADE,
    is_deleted   BOOLEAN NOT NULL DEFAULT false,
    PRIMARY KEY (room_type_id, amenity_id)
);

CREATE TABLE IF NOT EXISTS rooms (
    id           UUID             PRIMARY KEY DEFAULT gen_random_uuid(),
    room_type_id UUID             NOT NULL REFERENCES room_types(id) ON DELETE CASCADE,
    room_number  VARCHAR(20)      NOT NULL,
    floor        SMALLINT,
    status       room_status_enum NOT NULL DEFAULT 'AVAILABLE',
    is_deleted   BOOLEAN          NOT NULL DEFAULT false,
    UNIQUE (room_type_id, room_number)
);

CREATE TABLE IF NOT EXISTS pricing_rules (
    id              UUID                  PRIMARY KEY DEFAULT gen_random_uuid(),
    room_type_id    UUID                  NOT NULL REFERENCES room_types(id) ON DELETE CASCADE,
    type            pricing_type_enum     NOT NULL,
    start_date      DATE                  NOT NULL,
    end_date        DATE                  NOT NULL,
    adjustment_type adjustment_type_enum  NOT NULL,
    price_value     DECIMAL(12,2)         NOT NULL,
    priority        SMALLINT              NOT NULL,
    is_deleted      BOOLEAN               NOT NULL DEFAULT false
);


-- ============================================================
-- SEED DATA
-- ============================================================

-- ────────────────────────────────────────────────────────────
-- 1. Địa lý: Hà Nội → Hoàn Kiếm → Hàng Bạc
-- ────────────────────────────────────────────────────────────
INSERT INTO provinces (code, name, name_en, full_name) VALUES
    ('01', 'Hà Nội', 'Ha Noi', 'Thành phố Hà Nội')
ON CONFLICT (code) DO NOTHING;

INSERT INTO districts (code, province_code, name, name_en, full_name) VALUES
    ('001', '01', 'Hoàn Kiếm', 'Hoan Kiem', 'Quận Hoàn Kiếm')
ON CONFLICT (code) DO NOTHING;

INSERT INTO wards (code, district_code, name, name_en, full_name) VALUES
    ('00010', '001', 'Hàng Bạc', 'Hang Bac', 'Phường Hàng Bạc')
ON CONFLICT (code) DO NOTHING;


-- ────────────────────────────────────────────────────────────
-- 2. Amenities (tiện ích dùng chung cho hotel và room type)
-- ────────────────────────────────────────────────────────────
INSERT INTO amenities (id, name, scope) VALUES
    -- HOTEL-level
    ('a1000000-0000-0000-0000-000000000001', 'Hồ bơi',              'HOTEL'),
    ('a1000000-0000-0000-0000-000000000002', 'Bãi đỗ xe',           'HOTEL'),
    ('a1000000-0000-0000-0000-000000000003', 'Nhà hàng',            'HOTEL'),
    ('a1000000-0000-0000-0000-000000000004', 'Phòng gym',           'HOTEL'),
    ('a1000000-0000-0000-0000-000000000005', 'Spa',                 'HOTEL'),
    -- ROOM_TYPE-level
    ('a2000000-0000-0000-0000-000000000001', 'Wifi miễn phí',       'ROOM_TYPE'),
    ('a2000000-0000-0000-0000-000000000002', 'Điều hòa',            'ROOM_TYPE'),
    ('a2000000-0000-0000-0000-000000000003', 'Ban công',            'ROOM_TYPE'),
    ('a2000000-0000-0000-0000-000000000004', 'Bồn tắm',             'ROOM_TYPE'),
    ('a2000000-0000-0000-0000-000000000005', 'Tầm nhìn ra hồ',      'ROOM_TYPE'),
    -- BOTH
    ('a3000000-0000-0000-0000-000000000001', 'Két an toàn',         'BOTH'),
    ('a3000000-0000-0000-0000-000000000002', 'Minibar',             'BOTH')
ON CONFLICT (name) DO NOTHING;


-- ────────────────────────────────────────────────────────────
-- 3. Hotel: Hanoi Heritage Hotel
-- ────────────────────────────────────────────────────────────
INSERT INTO hotels (id, tenant_id, name, description, address, province_code, district_code, ward_code, status)
VALUES (
    'b0000000-0000-0000-0000-000000000001',
    'c0000000-0000-0000-0000-000000000001',  -- tenant_id (logic ref tới user-service)
    'Hanoi Heritage Hotel',
    'Khách sạn 5 sao nằm ngay trung tâm phố cổ Hà Nội, cách Hồ Hoàn Kiếm 200m.',
    '12 Hàng Bạc',
    '01',
    '001',
    '00010',
    'APPROVED'
);


-- ────────────────────────────────────────────────────────────
-- 4. Hotel images (1 cover + 3 ảnh thường)
-- ────────────────────────────────────────────────────────────
INSERT INTO hotel_images (hotel_id, url, is_cover) VALUES
    ('b0000000-0000-0000-0000-000000000001', 'https://images.unsplash.com/photo-1566073771259-6a8506099945', true),
    ('b0000000-0000-0000-0000-000000000001', 'https://images.unsplash.com/photo-1582719508461-905c673771fd', false),
    ('b0000000-0000-0000-0000-000000000001', 'https://images.unsplash.com/photo-1445019980597-93fa8acb246c', false),
    ('b0000000-0000-0000-0000-000000000001', 'https://images.unsplash.com/photo-1520250497591-112f2f40a3f4', false);


-- ────────────────────────────────────────────────────────────
-- 5. Hotel amenities (tiện ích của khách sạn)
-- ────────────────────────────────────────────────────────────
INSERT INTO hotel_amenities (hotel_id, amenity_id) VALUES
    ('b0000000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000001'),  -- Hồ bơi
    ('b0000000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000002'),  -- Bãi đỗ xe
    ('b0000000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000003'),  -- Nhà hàng
    ('b0000000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000004'),  -- Phòng gym
    ('b0000000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000005');  -- Spa


-- ────────────────────────────────────────────────────────────
-- 6. Policies
-- ────────────────────────────────────────────────────────────
INSERT INTO policies (hotel_id, type, description) VALUES
    ('b0000000-0000-0000-0000-000000000001', 'CHECKIN',    'Nhận phòng từ 14:00. Early check-in theo yêu cầu, phụ thuộc vào tình trạng phòng trống.'),
    ('b0000000-0000-0000-0000-000000000001', 'CHECKOUT',   'Trả phòng trước 12:00. Late check-out tính thêm 50% giá phòng.'),
    ('b0000000-0000-0000-0000-000000000001', 'CANCELATION','Hủy miễn phí trước 48 giờ. Hủy trong vòng 48 giờ tính phí 1 đêm đầu tiên.'),
    ('b0000000-0000-0000-0000-000000000001', 'SMOKING',    'Khách sạn không hút thuốc. Phạt 500.000 VNĐ nếu vi phạm.'),
    ('b0000000-0000-0000-0000-000000000001', 'PAYMENT',    'Chấp nhận tiền mặt, thẻ tín dụng và chuyển khoản ngân hàng.');


-- ────────────────────────────────────────────────────────────
-- 7. Room Types (5 loại phòng)
-- ────────────────────────────────────────────────────────────
INSERT INTO room_types (id, hotel_id, name, description, base_price_per_night, max_guests, area, bed_counts, quantity)
VALUES
    (
        'd1000000-0000-0000-0000-000000000001',
        'b0000000-0000-0000-0000-000000000001',
        'Standard Room',
        'Phòng tiêu chuẩn thoải mái, view thành phố, phù hợp cho cặp đôi hoặc khách đi công tác.',
        1200000, 2, 28, 1, 10
    ),
    (
        'd1000000-0000-0000-0000-000000000002',
        'b0000000-0000-0000-0000-000000000001',
        'Deluxe Room',
        'Phòng Deluxe rộng rãi với nội thất cao cấp, ban công nhỏ nhìn ra phố cổ.',
        1800000, 2, 35, 1, 8
    ),
    (
        'd1000000-0000-0000-0000-000000000003',
        'b0000000-0000-0000-0000-000000000001',
        'Superior Twin Room',
        'Phòng Superior với 2 giường đơn, phù hợp cho 2 người đi du lịch cùng nhau.',
        2000000, 2, 32, 2, 6
    ),
    (
        'd1000000-0000-0000-0000-000000000004',
        'b0000000-0000-0000-0000-000000000001',
        'Junior Suite',
        'Phòng Suite nhỏ với phòng khách riêng, bồn tắm đứng, tầm nhìn ra Hồ Hoàn Kiếm.',
        3500000, 3, 55, 1, 4
    ),
    (
        'd1000000-0000-0000-0000-000000000005',
        'b0000000-0000-0000-0000-000000000001',
        'Presidential Suite',
        'Suite hạng sang rộng 80m², phòng ăn riêng, bồn tắm jacuzzi, tầm nhìn toàn cảnh phố cổ.',
        8500000, 4, 80, 1, 1
    );


-- ────────────────────────────────────────────────────────────
-- 8. Room type images (mỗi loại phòng 2 ảnh: 1 cover + 1 thường)
-- ────────────────────────────────────────────────────────────
INSERT INTO room_type_images (room_type_id, url, is_cover) VALUES
    -- Standard Room
    ('d1000000-0000-0000-0000-000000000001', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000001', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    -- Deluxe Room
    ('d1000000-0000-0000-0000-000000000002', 'https://images.unsplash.com/photo-1611892440504-42a792e24d32', true),
    ('d1000000-0000-0000-0000-000000000002', 'https://images.unsplash.com/photo-1618773928121-c32242e63f39', false),
    -- Superior Twin Room
    ('d1000000-0000-0000-0000-000000000003', 'https://images.unsplash.com/photo-1595576508898-0ad5c879a061', true),
    ('d1000000-0000-0000-0000-000000000003', 'https://images.unsplash.com/photo-1560185007-c5ca9d2c014d', false),
    -- Junior Suite
    ('d1000000-0000-0000-0000-000000000004', 'https://images.unsplash.com/photo-1590490360182-c33d57733427', true),
    ('d1000000-0000-0000-0000-000000000004', 'https://images.unsplash.com/photo-1578683010236-d716f9a3f461', false),
    -- Presidential Suite
    ('d1000000-0000-0000-0000-000000000005', 'https://images.unsplash.com/photo-1571896349842-33c89424de2d', true),
    ('d1000000-0000-0000-0000-000000000005', 'https://images.unsplash.com/photo-1565183997392-2f6f122e5912', false);


-- ────────────────────────────────────────────────────────────
-- 9. Room type amenities
-- ────────────────────────────────────────────────────────────
INSERT INTO room_type_amenities (room_type_id, amenity_id) VALUES
    -- Standard Room: Wifi, Điều hòa
    ('d1000000-0000-0000-0000-000000000001', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000001', 'a2000000-0000-0000-0000-000000000002'),
    -- Deluxe Room: Wifi, Điều hòa, Ban công
    ('d1000000-0000-0000-0000-000000000002', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000002', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000002', 'a2000000-0000-0000-0000-000000000003'),
    -- Superior Twin Room: Wifi, Điều hòa, Két an toàn
    ('d1000000-0000-0000-0000-000000000003', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000003', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000003', 'a3000000-0000-0000-0000-000000000001'),
    -- Junior Suite: Wifi, Điều hòa, Ban công, Bồn tắm, Tầm nhìn ra hồ
    ('d1000000-0000-0000-0000-000000000004', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000004', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000004', 'a2000000-0000-0000-0000-000000000003'),
    ('d1000000-0000-0000-0000-000000000004', 'a2000000-0000-0000-0000-000000000004'),
    ('d1000000-0000-0000-0000-000000000004', 'a2000000-0000-0000-0000-000000000005'),
    -- Presidential Suite: Wifi, Điều hòa, Ban công, Bồn tắm, Tầm nhìn ra hồ, Két an toàn, Minibar
    ('d1000000-0000-0000-0000-000000000005', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000005', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000005', 'a2000000-0000-0000-0000-000000000003'),
    ('d1000000-0000-0000-0000-000000000005', 'a2000000-0000-0000-0000-000000000004'),
    ('d1000000-0000-0000-0000-000000000005', 'a2000000-0000-0000-0000-000000000005'),
    ('d1000000-0000-0000-0000-000000000005', 'a3000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000005', 'a3000000-0000-0000-0000-000000000002');


-- ────────────────────────────────────────────────────────────
-- 10. Pricing rules (giá cuối tuần cho một số loại phòng)
-- ────────────────────────────────────────────────────────────
INSERT INTO pricing_rules (room_type_id, type, start_date, end_date, adjustment_type, price_value, priority)
VALUES
    -- Deluxe Room: tăng 10% vào cuối tuần
    ('d1000000-0000-0000-0000-000000000002', 'WEEKEND',  '2025-01-01', '2025-12-31', 'PERCENTAGE_INCREASE', 10.00, 2),
    -- Junior Suite: giá đặc biệt dịp Tết
    ('d1000000-0000-0000-0000-000000000004', 'SPECIAL',  '2026-01-28', '2026-02-05', 'FIXED_PRICE',         5000000.00, 3),
    -- Presidential Suite: tăng 15% mùa hè
    ('d1000000-0000-0000-0000-000000000005', 'SEASONAL', '2025-06-01', '2025-08-31', 'PERCENTAGE_INCREASE', 15.00, 1);


-- ────────────────────────────────────────────────────────────
-- VERIFY — kiểm tra nhanh sau khi chạy
-- ────────────────────────────────────────────────────────────
SELECT 'provinces'         AS tbl, COUNT(*) FROM provinces
UNION ALL
SELECT 'districts',                COUNT(*) FROM districts
UNION ALL
SELECT 'wards',                    COUNT(*) FROM wards
UNION ALL
SELECT 'amenities',                COUNT(*) FROM amenities
UNION ALL
SELECT 'hotels',                   COUNT(*) FROM hotels
UNION ALL
SELECT 'hotel_images',             COUNT(*) FROM hotel_images
UNION ALL
SELECT 'hotel_amenities',          COUNT(*) FROM hotel_amenities
UNION ALL
SELECT 'policies',                 COUNT(*) FROM policies
UNION ALL
SELECT 'room_types',               COUNT(*) FROM room_types
UNION ALL
SELECT 'room_type_images',         COUNT(*) FROM room_type_images
UNION ALL
SELECT 'room_type_amenities',      COUNT(*) FROM room_type_amenities
UNION ALL
SELECT 'pricing_rules',            COUNT(*) FROM pricing_rules;