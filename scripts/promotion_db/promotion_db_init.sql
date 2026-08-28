-- =====================================================
-- EXTENSIONS
-- =====================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =====================================================
-- PROMOTIONS
-- =====================================================

CREATE TABLE promotions (
                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                            tenant_id UUID,

                            name VARCHAR(150) NOT NULL,
                            description TEXT,

                            type VARCHAR(50) NOT NULL,
                            discount_type VARCHAR(30) NOT NULL,

                            discount_value NUMERIC(12,2) NOT NULL,
                            max_discount_amount NUMERIC(12,2),

                            min_booking_amount NUMERIC(12,2),
                            min_nights INT,

                            start_at TIMESTAMPTZ NOT NULL,
                            end_at TIMESTAMPTZ NOT NULL,

                            status VARCHAR(30) NOT NULL,

                            total_usage_limit INT,
                            per_user_usage_limit INT,
                            current_usage_count INT NOT NULL DEFAULT 0,

                            stackable BOOLEAN NOT NULL DEFAULT FALSE,

                            created_by UUID,
                            updated_by UUID,

                            is_deleted BOOLEAN NOT NULL DEFAULT FALSE,

                            created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            deleted_at TIMESTAMPTZ
);

-- promotions
CREATE INDEX idx_promotions_status_time
    ON promotions(status, start_at, end_at);

CREATE INDEX idx_promotions_tenant_id
    ON promotions(tenant_id);

CREATE INDEX idx_promotions_type
    ON promotions(type);

CREATE INDEX idx_promotions_deleted
    ON promotions(is_deleted);

-- =====================================================
-- PROMOTION SCOPES
-- =====================================================

CREATE TABLE promotion_scopes (

                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                  promotion_id UUID NOT NULL,

                                  scope_type VARCHAR(30) NOT NULL,

                                  scope_ref_id UUID,

                                  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                  CONSTRAINT fk_scope_promotion
                                      FOREIGN KEY (promotion_id)
                                          REFERENCES promotions(id)
                                          ON DELETE CASCADE
);

-- promotion_scopes
CREATE INDEX idx_promotion_scopes_promotion_id
    ON promotion_scopes(promotion_id);

CREATE INDEX idx_promotion_scopes_type_ref
    ON promotion_scopes(scope_type, scope_ref_id);

-- =====================================================
-- COUPONS
-- =====================================================

CREATE TABLE coupons (

                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                         promotion_id UUID NOT NULL,

                         code VARCHAR(50) NOT NULL,

                         status VARCHAR(30) NOT NULL,

                         usage_limit INT,

                         current_usage_count INT NOT NULL DEFAULT 0,

                         is_deleted BOOLEAN NOT NULL DEFAULT FALSE,

                         created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                         CONSTRAINT fk_coupon_promotion
                             FOREIGN KEY (promotion_id)
                                 REFERENCES promotions(id)
                                 ON DELETE CASCADE,

                         CONSTRAINT uq_coupon_code
                             UNIQUE(code)
);

-- coupons
CREATE INDEX idx_coupons_status
    ON coupons(status);

-- =====================================================
-- PROMOTION CONDITIONS
-- =====================================================

CREATE TABLE promotion_conditions (

                                      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                      promotion_id UUID NOT NULL,

                                      condition_type VARCHAR(50) NOT NULL,

                                      operator VARCHAR(20) NOT NULL,

                                      condition_value VARCHAR(100) NOT NULL,

                                      created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                      CONSTRAINT fk_condition_promotion
                                          FOREIGN KEY (promotion_id)
                                              REFERENCES promotions(id)
                                              ON DELETE CASCADE
);

-- promotion_conditions
CREATE INDEX idx_promotion_conditions_promotion_type
    ON promotion_conditions(promotion_id, condition_type);

-- =====================================================
-- PROMOTION USAGES
-- =====================================================

CREATE TABLE promotion_usages (

                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                  promotion_id UUID NOT NULL,

                                  coupon_id UUID,

                                  booking_id UUID NOT NULL,

                                  user_id UUID NOT NULL,

                                  hotel_id UUID NOT NULL,

                                  discount_amount NUMERIC(12,2) NOT NULL,

                                  booking_amount NUMERIC(12,2) NOT NULL,

                                  used_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                  status VARCHAR(30) NOT NULL,

                                  idempotency_key VARCHAR(100),

                                  CONSTRAINT fk_usage_promotion
                                      FOREIGN KEY (promotion_id)
                                          REFERENCES promotions(id),

                                  CONSTRAINT fk_usage_coupon
                                      FOREIGN KEY (coupon_id)
                                          REFERENCES coupons(id),

                                  CONSTRAINT uq_booking_promotion
                                      UNIQUE(booking_id, promotion_id),

                                  CONSTRAINT uq_usage_idempotency
                                      UNIQUE(idempotency_key)
);

-- promotion_usages
CREATE INDEX idx_promotion_usages_promotion_id
    ON promotion_usages(promotion_id);

CREATE INDEX idx_promotion_usages_user_promotion
    ON promotion_usages(user_id, promotion_id);

CREATE INDEX idx_promotion_usages_booking_id
    ON promotion_usages(booking_id);

CREATE INDEX idx_promotion_usages_hotel_id
    ON promotion_usages(hotel_id);

-- =====================================================
-- PROMOTION AUDIT LOGS
-- =====================================================

CREATE TABLE promotion_audit_logs (

                                      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                      promotion_id UUID NOT NULL,

                                      action VARCHAR(50) NOT NULL,

                                      actor_id UUID,

                                      old_value JSONB,

                                      new_value JSONB,

                                      created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                      CONSTRAINT fk_audit_promotion
                                          FOREIGN KEY (promotion_id)
                                              REFERENCES promotions(id)
                                              ON DELETE CASCADE
);

-- promotion_audit_logs
CREATE INDEX idx_promotion_audit_logs_promotion
    ON promotion_audit_logs(promotion_id);

CREATE INDEX idx_promotion_audit_logs_created_at
    ON promotion_audit_logs(created_at);


-- ────────────────────────────────────────────────────────────
-- BẢNG outbox_events
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS outbox_events (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(100),
    topic         VARCHAR(100) NOT NULL,
    payload       JSONB       NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    published     BOOLEAN     NOT NULL DEFAULT false,   
    created_at    TIMESTAMP   NOT NULL DEFAULT NOW(),
    published_at  TIMESTAMP,

    retry_count   INTEGER     NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP,
    locked_until  TIMESTAMP,
    is_deleted    BOOLEAN     NOT NULL DEFAULT false,
    traceparent   VARCHAR(55),
    
    CONSTRAINT chk_status CHECK (status IN ('PENDING','PROCESSING','PUBLISHED','DEAD_LETTER'))
);

-- =====================================================
-- SAMPLE SEED DATA FOR TESTING
-- =====================================================

INSERT INTO promotions (
    id, name, description, type, discount_type, discount_value, max_discount_amount,
    min_booking_amount, min_nights, start_at, end_at, status, total_usage_limit, per_user_usage_limit, current_usage_count, stackable
) VALUES (
    'e1000000-0000-0000-0000-000000000001',
    'Khuyến mãi chào hè 2026',
    'Giảm 10% tối đa 500,000 VND cho đơn đặt phòng từ 1,000,000 VND khi sử dụng mã HE2026',
    'COUPON',
    'PERCENTAGE',
    10.00,
    500000.00,
    1000000.00,
    1,
    '2026-01-01 00:00:00+00',
    '2027-12-31 23:59:59+00',
    'ACTIVE',
    1000,
    5,
    0,
    FALSE
) ON CONFLICT (id) DO NOTHING;

INSERT INTO coupons (
    id, promotion_id, code, status, usage_limit, current_usage_count
) VALUES (
    'c1000000-0000-0000-0000-000000000001',
    'e1000000-0000-0000-0000-000000000001',
    'HE2026',
    'ACTIVE',
    1000,
    0
) ON CONFLICT (code) DO NOTHING;

INSERT INTO promotion_scopes (
    id, promotion_id, scope_type, scope_ref_id
) VALUES (
    's1000000-0000-0000-0000-000000000001',
    'e1000000-0000-0000-0000-000000000001',
    'SYSTEM',
    NULL
) ON CONFLICT (id) DO NOTHING;

