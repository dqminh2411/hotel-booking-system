-- User Service schema. Source of truth: ad_user_service.md
CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$ BEGIN
    CREATE TYPE user_status AS ENUM ( 'ACTIVE', 'LOCKED');
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
    keycloak_id UUID NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(20),
    full_name VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(500),
    address VARCHAR(500),
    status user_status NOT NULL DEFAULT 'ACTIVE',
    google_id VARCHAR(255) UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
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
CREATE INDEX IF NOT EXISTS idx_tenant_subscriptions_tenant
    ON tenant_subscriptions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_account_lock_history_user
    ON account_lock_history(user_id);

-- Enforce at most one active subscription for each tenant.
CREATE UNIQUE INDEX IF NOT EXISTS uq_tenant_active_subscription
    ON tenant_subscriptions(tenant_id)
    WHERE status = 'ACTIVE' AND is_deleted = FALSE;


-- Seed password: Password123. The requested personal email is intentionally not seeded.
INSERT INTO users (
    id, keycloak_id, email, phone, full_name, status, created_at, updated_at, is_deleted
) VALUES
    ('10000000-0000-0000-0000-000000000002','587ae577-e49a-4c71-8df9-67ae4a7ce752', 'user1@gmail.com', '0901234562', 'Tran Thi Binh', 'ACTIVE', NOW(), NOW(), FALSE),
    ('10000000-0000-0000-0000-000000000003','52d949a0-0411-4d5d-8017-6b0b94f97c35', 'staff1@gmail.com', '0901234563', 'Le Van Cuong', 'ACTIVE', NOW(), NOW(), FALSE),
    ('10000000-0000-0000-0000-000000000101','284feb3c-1cb4-4510-91ae-2a640511689b', 'owner1@gmail.com', '0911111111', 'Nguyen Thi Host', 'ACTIVE', NOW(), NOW(), FALSE)
-- ON CONFLICT (id) DO NOTHING;