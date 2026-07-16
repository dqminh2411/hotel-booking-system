-- User Service schema. Source of truth: ad_user_service.md
CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$ BEGIN
    CREATE TYPE user_status AS ENUM ('UNVERIFIED', 'ACTIVE', 'LOCKED');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE tenant_status AS ENUM ('ACTIVE', 'SUSPENDED');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE billing_cycle AS ENUM ('MONTHLY', 'YEARLY');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE subscription_status AS ENUM ('ACTIVE', 'EXPIRED', 'CANCELLED', 'SUSPENDED');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(20),
    password_hash VARCHAR(255),
    full_name VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(500),
    address VARCHAR(500),
    status user_status NOT NULL DEFAULT 'UNVERIFIED',
    google_id VARCHAR(255) UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS auth_providers (
    code VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS user_auth_providers (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    auth_provider_code VARCHAR(20) NOT NULL REFERENCES auth_providers(code) ON DELETE CASCADE,
    provider_user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (user_id, auth_provider_code)
);


CREATE TABLE IF NOT EXISTS tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id UUID NOT NULL UNIQUE REFERENCES users(id),
    name VARCHAR(255) NOT NULL,
    status tenant_status NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS subscription_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    price DECIMAL(12,2) NOT NULL DEFAULT 0,
    billing_cycle billing_cycle NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS tenant_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    subscription_plan_id UUID NOT NULL REFERENCES subscription_plans(id),
    status subscription_status NOT NULL DEFAULT 'ACTIVE',
    started_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS account_lock_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    locked_by UUID NOT NULL REFERENCES users(id),
    reason VARCHAR(500) NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    unlocked_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_user_roles_user ON user_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_role ON user_roles(role_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_hash ON refresh_tokens(token_hash);
CREATE INDEX IF NOT EXISTS idx_tenant_subscriptions_tenant
    ON tenant_subscriptions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_account_lock_history_user
    ON account_lock_history(user_id);

-- Enforce at most one active subscription for each tenant.
CREATE UNIQUE INDEX IF NOT EXISTS uq_tenant_active_subscription
    ON tenant_subscriptions(tenant_id)
    WHERE status = 'ACTIVE' AND is_deleted = FALSE;

INSERT INTO roles (id, name, description) VALUES
    ('00000000-0000-0000-0000-000000000001', 'CUSTOMER', 'Khách hàng đặt phòng'),
    ('00000000-0000-0000-0000-000000000002', 'HOTEL_STAFF', 'Nhân viên khách sạn'),
    ('00000000-0000-0000-0000-000000000003', 'HOTEL_OWNER', 'Chủ khách sạn'),
    ('00000000-0000-0000-0000-000000000004', 'PLATFORM_ADMIN', 'Quản trị viên nền tảng')
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    is_deleted = FALSE;

-- Seed password: Password123. The requested personal email is intentionally not seeded.
INSERT INTO users (
    id, email, phone, password_hash, full_name, status, created_at, updated_at, is_deleted
) VALUES
    ('10000000-0000-0000-0000-000000000002', 'binh.tran@email.com', '0901234562',
     crypt('Password123', gen_salt('bf', 12)), 'Tran Thi Binh', 'ACTIVE', NOW(), NOW(), FALSE),
    ('10000000-0000-0000-0000-000000000003', 'cuong.le@email.com', '0901234563',
     crypt('Password123', gen_salt('bf', 12)), 'Le Van Cuong', 'ACTIVE', NOW(), NOW(), FALSE),
    ('10000000-0000-0000-0000-000000000101', 'host1@hotel.com', '0911111111',
     crypt('Password123', gen_salt('bf', 12)), 'Nguyen Thi Host', 'ACTIVE', NOW(), NOW(), FALSE),
    ('10000000-0000-0000-0000-000000000104', 'vnkien28082004@gmail.com', '0911111111',
     crypt('Password123', gen_salt('bf', 12)), 'Vu Nhan Kien', 'ACTIVE', NOW(), NOW(), FALSE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO user_roles (user_id, role_id) VALUES
    ('10000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001'),
    ('10000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001'),
    ('10000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-000000000003'),
    ('10000000-0000-0000-0000-000000000104', '00000000-0000-0000-0000-000000000001')
ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO auth_providers(code,name) VALUES
    ('GOOGLE','Google');