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
-- 1. Địa lý: Hà Nội (Hoàn Kiếm) & Hồ Chí Minh (Quận 1)
-- ────────────────────────────────────────────────────────────
INSERT INTO provinces (code, name, name_en, full_name) VALUES
    ('01', 'Hà Nội', 'Ha Noi', 'Thành phố Hà Nội'),
    ('79', 'Hồ Chí Minh', 'Ho Chi Minh', 'Thành phố Hồ Chí Minh')
ON CONFLICT (code) DO NOTHING;

INSERT INTO districts (code, province_code, name, name_en, full_name) VALUES
    ('001', '01', 'Hoàn Kiếm', 'Hoan Kiem', 'Quận Hoàn Kiếm'),
    ('760', '79', 'Quận 1', 'District 1', 'Quận 1')
ON CONFLICT (code) DO NOTHING;

INSERT INTO wards (code, district_code, name, name_en, full_name) VALUES
    ('00010', '001', 'Hàng Bạc', 'Hang Bac', 'Phường Hàng Bạc'),
    ('26734', '760', 'Bến Nghé', 'Ben Nghe', 'Phường Bến Nghé')
ON CONFLICT (code) DO NOTHING;

-- ────────────────────────────────────────────────────────────
-- 2. Amenities (tiện ích dùng chung cho hotel và room type)
-- ────────────────────────────────────────────────────────────
INSERT INTO amenities (id, name, scope) VALUES
    ('a1000000-0000-0000-0000-000000000001', 'Hồ bơi',              'HOTEL'),
    ('a1000000-0000-0000-0000-000000000002', 'Bãi đỗ xe',           'HOTEL'),
    ('a1000000-0000-0000-0000-000000000003', 'Nhà hàng',            'HOTEL'),
    ('a1000000-0000-0000-0000-000000000004', 'Phòng gym',           'HOTEL'),
    ('a1000000-0000-0000-0000-000000000005', 'Spa',                 'HOTEL'),
    ('a2000000-0000-0000-0000-000000000001', 'Wifi miễn phí',       'ROOM_TYPE'),
    ('a2000000-0000-0000-0000-000000000002', 'Điều hòa',            'ROOM_TYPE'),
    ('a2000000-0000-0000-0000-000000000003', 'Ban công',            'ROOM_TYPE'),
    ('a2000000-0000-0000-0000-000000000004', 'Bồn tắm',             'ROOM_TYPE'),
    ('a2000000-0000-0000-0000-000000000005', 'Tầm nhìn ra hồ',      'ROOM_TYPE'),
    ('a3000000-0000-0000-0000-000000000001', 'Két an toàn',         'BOTH'),
    ('a3000000-0000-0000-0000-000000000002', 'Minibar',             'BOTH')
ON CONFLICT (name) DO NOTHING;

-- ────────────────────────────────────────────────────────────
-- 3. Hotels (12 hotels)
-- ────────────────────────────────────────────────────────────
INSERT INTO hotels (id, tenant_id, name, description, address, province_code, district_code, ward_code, status) VALUES
    ('b0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'Hanoi Hotel 1', 'Khách sạn đẳng cấp tại Hà Nội số 1', '11 Hàng Bạc', '01', '001', '00010', 'APPROVED'),
    ('b0000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000002', 'Hanoi Hotel 2', 'Khách sạn đẳng cấp tại Hà Nội số 2', '12 Hàng Bạc', '01', '001', '00010', 'APPROVED'),
    ('b0000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000003', 'Hanoi Hotel 3', 'Khách sạn đẳng cấp tại Hà Nội số 3', '13 Hàng Bạc', '01', '001', '00010', 'APPROVED'),
    ('b0000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000004', 'Hanoi Hotel 4', 'Khách sạn đẳng cấp tại Hà Nội số 4', '14 Hàng Bạc', '01', '001', '00010', 'APPROVED'),
    ('b0000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000005', 'Hanoi Hotel 5', 'Khách sạn đẳng cấp tại Hà Nội số 5', '15 Hàng Bạc', '01', '001', '00010', 'APPROVED'),
    ('b0000000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000006', 'Hanoi Hotel 6', 'Khách sạn đẳng cấp tại Hà Nội số 6', '16 Hàng Bạc', '01', '001', '00010', 'APPROVED'),
    ('b0000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000007', 'Saigon Hotel 1', 'Khách sạn đẳng cấp tại Hồ Chí Minh số 1', '101 Nguyễn Huệ', '79', '760', '26734', 'APPROVED'),
    ('b0000000-0000-0000-0000-000000000008', 'c0000000-0000-0000-0000-000000000008', 'Saigon Hotel 2', 'Khách sạn đẳng cấp tại Hồ Chí Minh số 2', '102 Nguyễn Huệ', '79', '760', '26734', 'APPROVED'),
    ('b0000000-0000-0000-0000-000000000009', 'c0000000-0000-0000-0000-000000000009', 'Saigon Hotel 3', 'Khách sạn đẳng cấp tại Hồ Chí Minh số 3', '103 Nguyễn Huệ', '79', '760', '26734', 'APPROVED'),
    ('b0000000-0000-0000-0000-000000000010', 'c0000000-0000-0000-0000-000000000010', 'Saigon Hotel 4', 'Khách sạn đẳng cấp tại Hồ Chí Minh số 4', '104 Nguyễn Huệ', '79', '760', '26734', 'APPROVED'),
    ('b0000000-0000-0000-0000-000000000011', 'c0000000-0000-0000-0000-000000000011', 'Saigon Hotel 5', 'Khách sạn đẳng cấp tại Hồ Chí Minh số 5', '105 Nguyễn Huệ', '79', '760', '26734', 'APPROVED'),
    ('b0000000-0000-0000-0000-000000000012', 'c0000000-0000-0000-0000-000000000012', 'Saigon Hotel 6', 'Khách sạn đẳng cấp tại Hồ Chí Minh số 6', '106 Nguyễn Huệ', '79', '760', '26734', 'APPROVED');

-- ────────────────────────────────────────────────────────────
-- 4. Hotel images
-- ────────────────────────────────────────────────────────────
INSERT INTO hotel_images (hotel_id, url, is_cover) VALUES
    ('b0000000-0000-0000-0000-000000000001', 'https://images.unsplash.com/photo-1566073771259-6a8506099945', true),
    ('b0000000-0000-0000-0000-000000000001', 'https://images.unsplash.com/photo-1582719508461-905c673771fd', false),
    ('b0000000-0000-0000-0000-000000000002', 'https://images.unsplash.com/photo-1566073771259-6a8506099945', true),
    ('b0000000-0000-0000-0000-000000000002', 'https://images.unsplash.com/photo-1582719508461-905c673771fd', false),
    ('b0000000-0000-0000-0000-000000000003', 'https://images.unsplash.com/photo-1566073771259-6a8506099945', true),
    ('b0000000-0000-0000-0000-000000000003', 'https://images.unsplash.com/photo-1582719508461-905c673771fd', false),
    ('b0000000-0000-0000-0000-000000000004', 'https://images.unsplash.com/photo-1566073771259-6a8506099945', true),
    ('b0000000-0000-0000-0000-000000000004', 'https://images.unsplash.com/photo-1582719508461-905c673771fd', false),
    ('b0000000-0000-0000-0000-000000000005', 'https://images.unsplash.com/photo-1566073771259-6a8506099945', true),
    ('b0000000-0000-0000-0000-000000000005', 'https://images.unsplash.com/photo-1582719508461-905c673771fd', false),
    ('b0000000-0000-0000-0000-000000000006', 'https://images.unsplash.com/photo-1566073771259-6a8506099945', true),
    ('b0000000-0000-0000-0000-000000000006', 'https://images.unsplash.com/photo-1582719508461-905c673771fd', false),
    ('b0000000-0000-0000-0000-000000000007', 'https://images.unsplash.com/photo-1566073771259-6a8506099945', true),
    ('b0000000-0000-0000-0000-000000000007', 'https://images.unsplash.com/photo-1582719508461-905c673771fd', false),
    ('b0000000-0000-0000-0000-000000000008', 'https://images.unsplash.com/photo-1566073771259-6a8506099945', true),
    ('b0000000-0000-0000-0000-000000000008', 'https://images.unsplash.com/photo-1582719508461-905c673771fd', false),
    ('b0000000-0000-0000-0000-000000000009', 'https://images.unsplash.com/photo-1566073771259-6a8506099945', true),
    ('b0000000-0000-0000-0000-000000000009', 'https://images.unsplash.com/photo-1582719508461-905c673771fd', false),
    ('b0000000-0000-0000-0000-000000000010', 'https://images.unsplash.com/photo-1566073771259-6a8506099945', true),
    ('b0000000-0000-0000-0000-000000000010', 'https://images.unsplash.com/photo-1582719508461-905c673771fd', false),
    ('b0000000-0000-0000-0000-000000000011', 'https://images.unsplash.com/photo-1566073771259-6a8506099945', true),
    ('b0000000-0000-0000-0000-000000000011', 'https://images.unsplash.com/photo-1582719508461-905c673771fd', false),
    ('b0000000-0000-0000-0000-000000000012', 'https://images.unsplash.com/photo-1566073771259-6a8506099945', true),
    ('b0000000-0000-0000-0000-000000000012', 'https://images.unsplash.com/photo-1582719508461-905c673771fd', false);

-- ────────────────────────────────────────────────────────────
-- 5. Hotel amenities
-- ────────────────────────────────────────────────────────────
INSERT INTO hotel_amenities (hotel_id, amenity_id) VALUES
    ('b0000000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000001'),
    ('b0000000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000002'),
    ('b0000000-0000-0000-0000-000000000002', 'a1000000-0000-0000-0000-000000000001'),
    ('b0000000-0000-0000-0000-000000000002', 'a1000000-0000-0000-0000-000000000002'),
    ('b0000000-0000-0000-0000-000000000003', 'a1000000-0000-0000-0000-000000000001'),
    ('b0000000-0000-0000-0000-000000000003', 'a1000000-0000-0000-0000-000000000002'),
    ('b0000000-0000-0000-0000-000000000004', 'a1000000-0000-0000-0000-000000000001'),
    ('b0000000-0000-0000-0000-000000000004', 'a1000000-0000-0000-0000-000000000002'),
    ('b0000000-0000-0000-0000-000000000005', 'a1000000-0000-0000-0000-000000000001'),
    ('b0000000-0000-0000-0000-000000000005', 'a1000000-0000-0000-0000-000000000002'),
    ('b0000000-0000-0000-0000-000000000006', 'a1000000-0000-0000-0000-000000000001'),
    ('b0000000-0000-0000-0000-000000000006', 'a1000000-0000-0000-0000-000000000002'),
    ('b0000000-0000-0000-0000-000000000007', 'a1000000-0000-0000-0000-000000000001'),
    ('b0000000-0000-0000-0000-000000000007', 'a1000000-0000-0000-0000-000000000002'),
    ('b0000000-0000-0000-0000-000000000008', 'a1000000-0000-0000-0000-000000000001'),
    ('b0000000-0000-0000-0000-000000000008', 'a1000000-0000-0000-0000-000000000002'),
    ('b0000000-0000-0000-0000-000000000009', 'a1000000-0000-0000-0000-000000000001'),
    ('b0000000-0000-0000-0000-000000000009', 'a1000000-0000-0000-0000-000000000002'),
    ('b0000000-0000-0000-0000-000000000010', 'a1000000-0000-0000-0000-000000000001'),
    ('b0000000-0000-0000-0000-000000000010', 'a1000000-0000-0000-0000-000000000002'),
    ('b0000000-0000-0000-0000-000000000011', 'a1000000-0000-0000-0000-000000000001'),
    ('b0000000-0000-0000-0000-000000000011', 'a1000000-0000-0000-0000-000000000002'),
    ('b0000000-0000-0000-0000-000000000012', 'a1000000-0000-0000-0000-000000000001'),
    ('b0000000-0000-0000-0000-000000000012', 'a1000000-0000-0000-0000-000000000002');

-- ────────────────────────────────────────────────────────────
-- 6. Policies
-- ────────────────────────────────────────────────────────────
INSERT INTO policies (hotel_id, type, description) VALUES
    ('b0000000-0000-0000-0000-000000000001', 'CHECKIN', 'Nhận phòng từ 14:00.'),
    ('b0000000-0000-0000-0000-000000000001', 'CHECKOUT', 'Trả phòng trước 12:00.'),
    ('b0000000-0000-0000-0000-000000000002', 'CHECKIN', 'Nhận phòng từ 14:00.'),
    ('b0000000-0000-0000-0000-000000000002', 'CHECKOUT', 'Trả phòng trước 12:00.'),
    ('b0000000-0000-0000-0000-000000000003', 'CHECKIN', 'Nhận phòng từ 14:00.'),
    ('b0000000-0000-0000-0000-000000000003', 'CHECKOUT', 'Trả phòng trước 12:00.'),
    ('b0000000-0000-0000-0000-000000000004', 'CHECKIN', 'Nhận phòng từ 14:00.'),
    ('b0000000-0000-0000-0000-000000000004', 'CHECKOUT', 'Trả phòng trước 12:00.'),
    ('b0000000-0000-0000-0000-000000000005', 'CHECKIN', 'Nhận phòng từ 14:00.'),
    ('b0000000-0000-0000-0000-000000000005', 'CHECKOUT', 'Trả phòng trước 12:00.'),
    ('b0000000-0000-0000-0000-000000000006', 'CHECKIN', 'Nhận phòng từ 14:00.'),
    ('b0000000-0000-0000-0000-000000000006', 'CHECKOUT', 'Trả phòng trước 12:00.'),
    ('b0000000-0000-0000-0000-000000000007', 'CHECKIN', 'Nhận phòng từ 14:00.'),
    ('b0000000-0000-0000-0000-000000000007', 'CHECKOUT', 'Trả phòng trước 12:00.'),
    ('b0000000-0000-0000-0000-000000000008', 'CHECKIN', 'Nhận phòng từ 14:00.'),
    ('b0000000-0000-0000-0000-000000000008', 'CHECKOUT', 'Trả phòng trước 12:00.'),
    ('b0000000-0000-0000-0000-000000000009', 'CHECKIN', 'Nhận phòng từ 14:00.'),
    ('b0000000-0000-0000-0000-000000000009', 'CHECKOUT', 'Trả phòng trước 12:00.'),
    ('b0000000-0000-0000-0000-000000000010', 'CHECKIN', 'Nhận phòng từ 14:00.'),
    ('b0000000-0000-0000-0000-000000000010', 'CHECKOUT', 'Trả phòng trước 12:00.'),
    ('b0000000-0000-0000-0000-000000000011', 'CHECKIN', 'Nhận phòng từ 14:00.'),
    ('b0000000-0000-0000-0000-000000000011', 'CHECKOUT', 'Trả phòng trước 12:00.'),
    ('b0000000-0000-0000-0000-000000000012', 'CHECKIN', 'Nhận phòng từ 14:00.'),
    ('b0000000-0000-0000-0000-000000000012', 'CHECKOUT', 'Trả phòng trước 12:00.');

-- ────────────────────────────────────────────────────────────
-- 7. Room Types (5 per hotel)
-- ────────────────────────────────────────────────────────────
INSERT INTO room_types (id, hotel_id, name, description, base_price_per_night, max_guests, area, bed_counts, quantity) VALUES
    ('d1000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 'Standard Room', 'Phòng Standard Room', 1200000, 2, 28, 1, 10),
    ('d1000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000001', 'Deluxe Room', 'Phòng Deluxe Room', 1800000, 2, 35, 1, 8),
    ('d1000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000001', 'Superior Twin Room', 'Phòng Superior Twin Room', 2000000, 2, 32, 2, 6),
    ('d1000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000001', 'Junior Suite', 'Phòng Junior Suite', 3500000, 3, 55, 1, 4),
    ('d1000000-0000-0000-0000-000000000005', 'b0000000-0000-0000-0000-000000000001', 'Presidential Suite', 'Phòng Presidential Suite', 8500000, 4, 80, 1, 1),
    ('d1000000-0000-0000-0000-000000000006', 'b0000000-0000-0000-0000-000000000002', 'Standard Room', 'Phòng Standard Room', 1200000, 2, 28, 1, 10),
    ('d1000000-0000-0000-0000-000000000007', 'b0000000-0000-0000-0000-000000000002', 'Deluxe Room', 'Phòng Deluxe Room', 1800000, 2, 35, 1, 8),
    ('d1000000-0000-0000-0000-000000000008', 'b0000000-0000-0000-0000-000000000002', 'Superior Twin Room', 'Phòng Superior Twin Room', 2000000, 2, 32, 2, 6),
    ('d1000000-0000-0000-0000-000000000009', 'b0000000-0000-0000-0000-000000000002', 'Junior Suite', 'Phòng Junior Suite', 3500000, 3, 55, 1, 4),
    ('d1000000-0000-0000-0000-000000000010', 'b0000000-0000-0000-0000-000000000002', 'Presidential Suite', 'Phòng Presidential Suite', 8500000, 4, 80, 1, 2),
    ('d1000000-0000-0000-0000-000000000011', 'b0000000-0000-0000-0000-000000000003', 'Standard Room', 'Phòng Standard Room', 1200000, 2, 28, 1, 10),
    ('d1000000-0000-0000-0000-000000000012', 'b0000000-0000-0000-0000-000000000003', 'Deluxe Room', 'Phòng Deluxe Room', 1800000, 2, 35, 1, 8),
    ('d1000000-0000-0000-0000-000000000013', 'b0000000-0000-0000-0000-000000000003', 'Superior Twin Room', 'Phòng Superior Twin Room', 2000000, 2, 32, 2, 6),
    ('d1000000-0000-0000-0000-000000000014', 'b0000000-0000-0000-0000-000000000003', 'Junior Suite', 'Phòng Junior Suite', 3500000, 3, 55, 1, 4),
    ('d1000000-0000-0000-0000-000000000015', 'b0000000-0000-0000-0000-000000000003', 'Presidential Suite', 'Phòng Presidential Suite', 8500000, 4, 80, 1, 2),
    ('d1000000-0000-0000-0000-000000000016', 'b0000000-0000-0000-0000-000000000004', 'Standard Room', 'Phòng Standard Room', 1200000, 2, 28, 1, 10),
    ('d1000000-0000-0000-0000-000000000017', 'b0000000-0000-0000-0000-000000000004', 'Deluxe Room', 'Phòng Deluxe Room', 1800000, 2, 35, 1, 8),
    ('d1000000-0000-0000-0000-000000000018', 'b0000000-0000-0000-0000-000000000004', 'Superior Twin Room', 'Phòng Superior Twin Room', 2000000, 2, 32, 2, 6),
    ('d1000000-0000-0000-0000-000000000019', 'b0000000-0000-0000-0000-000000000004', 'Junior Suite', 'Phòng Junior Suite', 3500000, 3, 55, 1, 4),
    ('d1000000-0000-0000-0000-000000000020', 'b0000000-0000-0000-0000-000000000004', 'Presidential Suite', 'Phòng Presidential Suite', 8500000, 4, 80, 1, 2),
    ('d1000000-0000-0000-0000-000000000021', 'b0000000-0000-0000-0000-000000000005', 'Standard Room', 'Phòng Standard Room', 1200000, 2, 28, 1, 10),
    ('d1000000-0000-0000-0000-000000000022', 'b0000000-0000-0000-0000-000000000005', 'Deluxe Room', 'Phòng Deluxe Room', 1800000, 2, 35, 1, 8),
    ('d1000000-0000-0000-0000-000000000023', 'b0000000-0000-0000-0000-000000000005', 'Superior Twin Room', 'Phòng Superior Twin Room', 2000000, 2, 32, 2, 6),
    ('d1000000-0000-0000-0000-000000000024', 'b0000000-0000-0000-0000-000000000005', 'Junior Suite', 'Phòng Junior Suite', 3500000, 3, 55, 1, 4),
    ('d1000000-0000-0000-0000-000000000025', 'b0000000-0000-0000-0000-000000000005', 'Presidential Suite', 'Phòng Presidential Suite', 8500000, 4, 80, 1, 2),
    ('d1000000-0000-0000-0000-000000000026', 'b0000000-0000-0000-0000-000000000006', 'Standard Room', 'Phòng Standard Room', 1200000, 2, 28, 1, 10),
    ('d1000000-0000-0000-0000-000000000027', 'b0000000-0000-0000-0000-000000000006', 'Deluxe Room', 'Phòng Deluxe Room', 1800000, 2, 35, 1, 8),
    ('d1000000-0000-0000-0000-000000000028', 'b0000000-0000-0000-0000-000000000006', 'Superior Twin Room', 'Phòng Superior Twin Room', 2000000, 2, 32, 2, 6),
    ('d1000000-0000-0000-0000-000000000029', 'b0000000-0000-0000-0000-000000000006', 'Junior Suite', 'Phòng Junior Suite', 3500000, 3, 55, 1, 4),
    ('d1000000-0000-0000-0000-000000000030', 'b0000000-0000-0000-0000-000000000006', 'Presidential Suite', 'Phòng Presidential Suite', 8500000, 4, 80, 1, 2),
    ('d1000000-0000-0000-0000-000000000031', 'b0000000-0000-0000-0000-000000000007', 'Standard Room', 'Phòng Standard Room', 1200000, 2, 28, 1, 10),
    ('d1000000-0000-0000-0000-000000000032', 'b0000000-0000-0000-0000-000000000007', 'Deluxe Room', 'Phòng Deluxe Room', 1800000, 2, 35, 1, 8),
    ('d1000000-0000-0000-0000-000000000033', 'b0000000-0000-0000-0000-000000000007', 'Superior Twin Room', 'Phòng Superior Twin Room', 2000000, 2, 32, 2, 6),
    ('d1000000-0000-0000-0000-000000000034', 'b0000000-0000-0000-0000-000000000007', 'Junior Suite', 'Phòng Junior Suite', 3500000, 3, 55, 1, 4),
    ('d1000000-0000-0000-0000-000000000035', 'b0000000-0000-0000-0000-000000000007', 'Presidential Suite', 'Phòng Presidential Suite', 8500000, 4, 80, 1, 2),
    ('d1000000-0000-0000-0000-000000000036', 'b0000000-0000-0000-0000-000000000008', 'Standard Room', 'Phòng Standard Room', 1200000, 2, 28, 1, 10),
    ('d1000000-0000-0000-0000-000000000037', 'b0000000-0000-0000-0000-000000000008', 'Deluxe Room', 'Phòng Deluxe Room', 1800000, 2, 35, 1, 8),
    ('d1000000-0000-0000-0000-000000000038', 'b0000000-0000-0000-0000-000000000008', 'Superior Twin Room', 'Phòng Superior Twin Room', 2000000, 2, 32, 2, 6),
    ('d1000000-0000-0000-0000-000000000039', 'b0000000-0000-0000-0000-000000000008', 'Junior Suite', 'Phòng Junior Suite', 3500000, 3, 55, 1, 4),
    ('d1000000-0000-0000-0000-000000000040', 'b0000000-0000-0000-0000-000000000008', 'Presidential Suite', 'Phòng Presidential Suite', 8500000, 4, 80, 1, 2),
    ('d1000000-0000-0000-0000-000000000041', 'b0000000-0000-0000-0000-000000000009', 'Standard Room', 'Phòng Standard Room', 1200000, 2, 28, 1, 10),
    ('d1000000-0000-0000-0000-000000000042', 'b0000000-0000-0000-0000-000000000009', 'Deluxe Room', 'Phòng Deluxe Room', 1800000, 2, 35, 1, 8),
    ('d1000000-0000-0000-0000-000000000043', 'b0000000-0000-0000-0000-000000000009', 'Superior Twin Room', 'Phòng Superior Twin Room', 2000000, 2, 32, 2, 6),
    ('d1000000-0000-0000-0000-000000000044', 'b0000000-0000-0000-0000-000000000009', 'Junior Suite', 'Phòng Junior Suite', 3500000, 3, 55, 1, 4),
    ('d1000000-0000-0000-0000-000000000045', 'b0000000-0000-0000-0000-000000000009', 'Presidential Suite', 'Phòng Presidential Suite', 8500000, 4, 80, 1, 2),
    ('d1000000-0000-0000-0000-000000000046', 'b0000000-0000-0000-0000-000000000010', 'Standard Room', 'Phòng Standard Room', 1200000, 2, 28, 1, 10),
    ('d1000000-0000-0000-0000-000000000047', 'b0000000-0000-0000-0000-000000000010', 'Deluxe Room', 'Phòng Deluxe Room', 1800000, 2, 35, 1, 8),
    ('d1000000-0000-0000-0000-000000000048', 'b0000000-0000-0000-0000-000000000010', 'Superior Twin Room', 'Phòng Superior Twin Room', 2000000, 2, 32, 2, 6),
    ('d1000000-0000-0000-0000-000000000049', 'b0000000-0000-0000-0000-000000000010', 'Junior Suite', 'Phòng Junior Suite', 3500000, 3, 55, 1, 4),
    ('d1000000-0000-0000-0000-000000000050', 'b0000000-0000-0000-0000-000000000010', 'Presidential Suite', 'Phòng Presidential Suite', 8500000, 4, 80, 1, 2),
    ('d1000000-0000-0000-0000-000000000051', 'b0000000-0000-0000-0000-000000000011', 'Standard Room', 'Phòng Standard Room', 1200000, 2, 28, 1, 10),
    ('d1000000-0000-0000-0000-000000000052', 'b0000000-0000-0000-0000-000000000011', 'Deluxe Room', 'Phòng Deluxe Room', 1800000, 2, 35, 1, 8),
    ('d1000000-0000-0000-0000-000000000053', 'b0000000-0000-0000-0000-000000000011', 'Superior Twin Room', 'Phòng Superior Twin Room', 2000000, 2, 32, 2, 6),
    ('d1000000-0000-0000-0000-000000000054', 'b0000000-0000-0000-0000-000000000011', 'Junior Suite', 'Phòng Junior Suite', 3500000, 3, 55, 1, 4),
    ('d1000000-0000-0000-0000-000000000055', 'b0000000-0000-0000-0000-000000000011', 'Presidential Suite', 'Phòng Presidential Suite', 8500000, 4, 80, 1, 2),
    ('d1000000-0000-0000-0000-000000000056', 'b0000000-0000-0000-0000-000000000012', 'Standard Room', 'Phòng Standard Room', 1200000, 2, 28, 1, 10),
    ('d1000000-0000-0000-0000-000000000057', 'b0000000-0000-0000-0000-000000000012', 'Deluxe Room', 'Phòng Deluxe Room', 1800000, 2, 35, 1, 8),
    ('d1000000-0000-0000-0000-000000000058', 'b0000000-0000-0000-0000-000000000012', 'Superior Twin Room', 'Phòng Superior Twin Room', 2000000, 2, 32, 2, 6),
    ('d1000000-0000-0000-0000-000000000059', 'b0000000-0000-0000-0000-000000000012', 'Junior Suite', 'Phòng Junior Suite', 3500000, 3, 55, 1, 4),
    ('d1000000-0000-0000-0000-000000000060', 'b0000000-0000-0000-0000-000000000012', 'Presidential Suite', 'Phòng Presidential Suite', 8500000, 4, 80, 1, 2);

-- ────────────────────────────────────────────────────────────
-- 8. Room type images
-- ────────────────────────────────────────────────────────────
INSERT INTO room_type_images (room_type_id, url, is_cover) VALUES
    ('d1000000-0000-0000-0000-000000000001', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000001', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000002', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000002', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000003', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000003', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000004', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000004', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000005', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000005', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000006', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000006', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000007', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000007', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000008', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000008', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000009', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000009', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000010', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000010', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000011', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000011', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000012', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000012', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000013', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000013', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000014', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000014', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000015', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000015', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000016', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000016', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000017', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000017', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000018', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000018', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000019', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000019', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000020', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000020', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000021', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000021', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000022', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000022', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000023', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000023', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000024', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000024', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000025', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000025', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000026', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000026', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000027', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000027', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000028', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000028', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000029', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000029', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000030', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000030', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000031', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000031', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000032', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000032', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000033', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000033', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000034', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000034', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000035', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000035', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000036', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000036', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000037', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000037', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000038', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000038', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000039', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000039', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000040', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000040', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000041', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000041', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000042', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000042', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000043', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000043', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000044', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000044', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000045', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000045', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000046', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000046', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000047', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000047', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000048', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000048', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000049', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000049', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000050', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000050', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000051', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000051', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000052', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000052', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000053', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000053', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000054', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000054', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000055', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000055', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000056', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000056', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000057', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000057', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000058', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000058', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000059', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000059', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false),
    ('d1000000-0000-0000-0000-000000000060', 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304', true),
    ('d1000000-0000-0000-0000-000000000060', 'https://images.unsplash.com/photo-1631049552057-403cdb8f0658', false);

-- ────────────────────────────────────────────────────────────
-- 9. Room type amenities
-- ────────────────────────────────────────────────────────────
INSERT INTO room_type_amenities (room_type_id, amenity_id) VALUES
    ('d1000000-0000-0000-0000-000000000001', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000001', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000002', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000002', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000003', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000003', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000004', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000004', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000005', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000005', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000006', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000006', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000007', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000007', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000008', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000008', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000009', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000009', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000010', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000010', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000011', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000011', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000012', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000012', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000013', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000013', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000014', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000014', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000015', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000015', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000016', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000016', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000017', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000017', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000018', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000018', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000019', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000019', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000020', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000020', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000021', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000021', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000022', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000022', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000023', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000023', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000024', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000024', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000025', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000025', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000026', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000026', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000027', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000027', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000028', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000028', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000029', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000029', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000030', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000030', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000031', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000031', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000032', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000032', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000033', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000033', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000034', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000034', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000035', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000035', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000036', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000036', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000037', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000037', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000038', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000038', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000039', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000039', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000040', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000040', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000041', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000041', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000042', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000042', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000043', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000043', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000044', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000044', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000045', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000045', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000046', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000046', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000047', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000047', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000048', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000048', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000049', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000049', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000050', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000050', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000051', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000051', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000052', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000052', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000053', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000053', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000054', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000054', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000055', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000055', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000056', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000056', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000057', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000057', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000058', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000058', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000059', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000059', 'a2000000-0000-0000-0000-000000000002'),
    ('d1000000-0000-0000-0000-000000000060', 'a2000000-0000-0000-0000-000000000001'),
    ('d1000000-0000-0000-0000-000000000060', 'a2000000-0000-0000-0000-000000000002');

-- ────────────────────────────────────────────────────────────
-- 10. Pricing rules
-- ────────────────────────────────────────────────────────────
INSERT INTO pricing_rules (room_type_id, type, start_date, end_date, adjustment_type, price_value, priority) VALUES
    ('d1000000-0000-0000-0000-000000000002', 'WEEKEND', '2025-01-01', '2025-12-31', 'PERCENTAGE_INCREASE', 10.00, 2),
    ('d1000000-0000-0000-0000-000000000004', 'SPECIAL', '2026-01-28', '2026-02-05', 'FIXED_PRICE', 5000000.00, 3),
    ('d1000000-0000-0000-0000-000000000005', 'SEASONAL', '2025-06-01', '2025-08-31', 'PERCENTAGE_INCREASE', 15.00, 1),
    ('d1000000-0000-0000-0000-000000000007', 'WEEKEND', '2025-01-01', '2025-12-31', 'PERCENTAGE_INCREASE', 10.00, 2),
    ('d1000000-0000-0000-0000-000000000009', 'SPECIAL', '2026-01-28', '2026-02-05', 'FIXED_PRICE', 5000000.00, 3),
    ('d1000000-0000-0000-0000-000000000010', 'SEASONAL', '2025-06-01', '2025-08-31', 'PERCENTAGE_INCREASE', 15.00, 1),
    ('d1000000-0000-0000-0000-000000000012', 'WEEKEND', '2025-01-01', '2025-12-31', 'PERCENTAGE_INCREASE', 10.00, 2),
    ('d1000000-0000-0000-0000-000000000014', 'SPECIAL', '2026-01-28', '2026-02-05', 'FIXED_PRICE', 5000000.00, 3),
    ('d1000000-0000-0000-0000-000000000015', 'SEASONAL', '2025-06-01', '2025-08-31', 'PERCENTAGE_INCREASE', 15.00, 1),
    ('d1000000-0000-0000-0000-000000000017', 'WEEKEND', '2025-01-01', '2025-12-31', 'PERCENTAGE_INCREASE', 10.00, 2),
    ('d1000000-0000-0000-0000-000000000019', 'SPECIAL', '2026-01-28', '2026-02-05', 'FIXED_PRICE', 5000000.00, 3),
    ('d1000000-0000-0000-0000-000000000020', 'SEASONAL', '2025-06-01', '2025-08-31', 'PERCENTAGE_INCREASE', 15.00, 1),
    ('d1000000-0000-0000-0000-000000000022', 'WEEKEND', '2025-01-01', '2025-12-31', 'PERCENTAGE_INCREASE', 10.00, 2),
    ('d1000000-0000-0000-0000-000000000024', 'SPECIAL', '2026-01-28', '2026-02-05', 'FIXED_PRICE', 5000000.00, 3),
    ('d1000000-0000-0000-0000-000000000025', 'SEASONAL', '2025-06-01', '2025-08-31', 'PERCENTAGE_INCREASE', 15.00, 1),
    ('d1000000-0000-0000-0000-000000000027', 'WEEKEND', '2025-01-01', '2025-12-31', 'PERCENTAGE_INCREASE', 10.00, 2),
    ('d1000000-0000-0000-0000-000000000029', 'SPECIAL', '2026-01-28', '2026-02-05', 'FIXED_PRICE', 5000000.00, 3),
    ('d1000000-0000-0000-0000-000000000030', 'SEASONAL', '2025-06-01', '2025-08-31', 'PERCENTAGE_INCREASE', 15.00, 1),
    ('d1000000-0000-0000-0000-000000000032', 'WEEKEND', '2025-01-01', '2025-12-31', 'PERCENTAGE_INCREASE', 10.00, 2),
    ('d1000000-0000-0000-0000-000000000034', 'SPECIAL', '2026-01-28', '2026-02-05', 'FIXED_PRICE', 5000000.00, 3),
    ('d1000000-0000-0000-0000-000000000035', 'SEASONAL', '2025-06-01', '2025-08-31', 'PERCENTAGE_INCREASE', 15.00, 1),
    ('d1000000-0000-0000-0000-000000000037', 'WEEKEND', '2025-01-01', '2025-12-31', 'PERCENTAGE_INCREASE', 10.00, 2),
    ('d1000000-0000-0000-0000-000000000039', 'SPECIAL', '2026-01-28', '2026-02-05', 'FIXED_PRICE', 5000000.00, 3),
    ('d1000000-0000-0000-0000-000000000040', 'SEASONAL', '2025-06-01', '2025-08-31', 'PERCENTAGE_INCREASE', 15.00, 1),
    ('d1000000-0000-0000-0000-000000000042', 'WEEKEND', '2025-01-01', '2025-12-31', 'PERCENTAGE_INCREASE', 10.00, 2),
    ('d1000000-0000-0000-0000-000000000044', 'SPECIAL', '2026-01-28', '2026-02-05', 'FIXED_PRICE', 5000000.00, 3),
    ('d1000000-0000-0000-0000-000000000045', 'SEASONAL', '2025-06-01', '2025-08-31', 'PERCENTAGE_INCREASE', 15.00, 1),
    ('d1000000-0000-0000-0000-000000000047', 'WEEKEND', '2025-01-01', '2025-12-31', 'PERCENTAGE_INCREASE', 10.00, 2),
    ('d1000000-0000-0000-0000-000000000049', 'SPECIAL', '2026-01-28', '2026-02-05', 'FIXED_PRICE', 5000000.00, 3),
    ('d1000000-0000-0000-0000-000000000050', 'SEASONAL', '2025-06-01', '2025-08-31', 'PERCENTAGE_INCREASE', 15.00, 1),
    ('d1000000-0000-0000-0000-000000000052', 'WEEKEND', '2025-01-01', '2025-12-31', 'PERCENTAGE_INCREASE', 10.00, 2),
    ('d1000000-0000-0000-0000-000000000054', 'SPECIAL', '2026-01-28', '2026-02-05', 'FIXED_PRICE', 5000000.00, 3),
    ('d1000000-0000-0000-0000-000000000055', 'SEASONAL', '2025-06-01', '2025-08-31', 'PERCENTAGE_INCREASE', 15.00, 1),
    ('d1000000-0000-0000-0000-000000000057', 'WEEKEND', '2025-01-01', '2025-12-31', 'PERCENTAGE_INCREASE', 10.00, 2),
    ('d1000000-0000-0000-0000-000000000059', 'SPECIAL', '2026-01-28', '2026-02-05', 'FIXED_PRICE', 5000000.00, 3),
    ('d1000000-0000-0000-0000-000000000060', 'SEASONAL', '2025-06-01', '2025-08-31', 'PERCENTAGE_INCREASE', 15.00, 1);
-- ────────────────────────────────────────────────────────────
-- 11. Rooms (phòng vật lý cụ thể theo từng loại phòng)
-- Tổng số phòng = SUM(room_types.quantity) = 10+8+6+4+1 = 29
-- ────────────────────────────────────────────────────────────
INSERT INTO rooms (id, room_type_id, room_number, floor, status) VALUES
    -- Standard Room (d1000000-...0001) — quantity = 10 — tầng 2-3
    ('e1000000-0000-0000-0000-000000000001', 'd1000000-0000-0000-0000-000000000001', '201', 2, 'AVAILABLE'),
    ('e1000000-0000-0000-0000-000000000002', 'd1000000-0000-0000-0000-000000000001', '202', 2, 'AVAILABLE'),
    ('e1000000-0000-0000-0000-000000000003', 'd1000000-0000-0000-0000-000000000001', '203', 2, 'AVAILABLE'),
    ('e1000000-0000-0000-0000-000000000004', 'd1000000-0000-0000-0000-000000000001', '204', 2, 'AVAILABLE'),
    ('e1000000-0000-0000-0000-000000000005', 'd1000000-0000-0000-0000-000000000001', '205', 2, 'AVAILABLE'),
    ('e1000000-0000-0000-0000-000000000006', 'd1000000-0000-0000-0000-000000000001', '301', 3, 'AVAILABLE'),
    ('e1000000-0000-0000-0000-000000000007', 'd1000000-0000-0000-0000-000000000001', '302', 3, 'AVAILABLE'),
    ('e1000000-0000-0000-0000-000000000008', 'd1000000-0000-0000-0000-000000000001', '303', 3, 'AVAILABLE'),
    ('e1000000-0000-0000-0000-000000000009', 'd1000000-0000-0000-0000-000000000001', '304', 3, 'AVAILABLE'),
    ('e1000000-0000-0000-0000-000000000010', 'd1000000-0000-0000-0000-000000000001', '305', 3, 'AVAILABLE'),

    -- Deluxe Room (d1000000-...0002) — quantity = 8 — tầng 4-5
    ('e1000000-0000-0000-0000-000000000011', 'd1000000-0000-0000-0000-000000000002', '401', 4, 'AVAILABLE'),
    ('e1000000-0000-0000-0000-000000000012', 'd1000000-0000-0000-0000-000000000002', '402', 4, 'AVAILABLE'),
    ('e1000000-0000-0000-0000-000000000013', 'd1000000-0000-0000-0000-000000000002', '403', 4, 'AVAILABLE'),
    ('e1000000-0000-0000-0000-000000000014', 'd1000000-0000-0000-0000-000000000002', '404', 4, 'AVAILABLE'),
    ('e1000000-0000-0000-0000-000000000015', 'd1000000-0000-0000-0000-000000000002', '501', 5, 'AVAILABLE'),
    ('e1000000-0000-0000-0000-000000000016', 'd1000000-0000-0000-0000-000000000002', '502', 5, 'AVAILABLE'),
    ('e1000000-0000-0000-0000-000000000017', 'd1000000-0000-0000-0000-000000000002', '503', 5, 'AVAILABLE'),
    ('e1000000-0000-0000-0000-000000000018', 'd1000000-0000-0000-0000-000000000002', '504', 5, 'AVAILABLE'),

    -- Superior Twin Room (d1000000-...0003) — quantity = 6 — tầng 6
    ('e1000000-0000-0000-0000-000000000019', 'd1000000-0000-0000-0000-000000000003', '601', 6, 'AVAILABLE'),
    ('e1000000-0000-0000-0000-000000000020', 'd1000000-0000-0000-0000-000000000003', '602', 6, 'AVAILABLE'),
    ('e1000000-0000-0000-0000-000000000021', 'd1000000-0000-0000-0000-000000000003', '603', 6, 'AVAILABLE'),
    ('e1000000-0000-0000-0000-000000000022', 'd1000000-0000-0000-0000-000000000003', '604', 6, 'AVAILABLE'),
    ('e1000000-0000-0000-0000-000000000023', 'd1000000-0000-0000-0000-000000000003', '605', 6, 'AVAILABLE'),
    ('e1000000-0000-0000-0000-000000000024', 'd1000000-0000-0000-0000-000000000003', '606', 6, 'AVAILABLE'),

    -- Junior Suite (d1000000-...0004) — quantity = 4 — tầng 7
    ('e1000000-0000-0000-0000-000000000025', 'd1000000-0000-0000-0000-000000000004', '701', 7, 'AVAILABLE'),
    ('e1000000-0000-0000-0000-000000000026', 'd1000000-0000-0000-0000-000000000004', '702', 7, 'AVAILABLE'),
    ('e1000000-0000-0000-0000-000000000027', 'd1000000-0000-0000-0000-000000000004', '703', 7, 'AVAILABLE'),
    ('e1000000-0000-0000-0000-000000000028', 'd1000000-0000-0000-0000-000000000004', '704', 7, 'AVAILABLE'),

    -- Presidential Suite (d1000000-...0005) — quantity = 1 — tầng 8 (tầng cao nhất, view toàn cảnh)
    ('e1000000-0000-0000-0000-000000000029', 'd1000000-0000-0000-0000-000000000005', '801', 8, 'AVAILABLE');

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
UNION ALL
SELECT 'rooms',                    COUNT(*) FROM rooms;