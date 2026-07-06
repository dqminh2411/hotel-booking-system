CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_user_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    payload JSONB,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_notifications_event_type CHECK (
        event_type IN (
            'BOOKING_CONFIRMED',
            'BOOKING_CANCELLED',
            'BOOKING_FAILED',
            'CHECKIN_REMINDER',
            'PAYMENT_CONFIRMED',
            'PAYMENT_FAILED',
            'PROMOTION'
        )
    ),
    CONSTRAINT chk_notifications_read_at CHECK (
        read_at IS NULL OR read_at >= created_at
    )
);

CREATE INDEX idx_notifications_recipient_created
    ON notifications (recipient_user_id, created_at DESC);
CREATE INDEX idx_notifications_recipient_read
    ON notifications (recipient_user_id, read_at);
CREATE INDEX idx_notifications_event_type
    ON notifications (event_type);
CREATE INDEX idx_notifications_created_at
    ON notifications (created_at DESC);

CREATE TABLE notification_delivery_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id UUID NOT NULL,
    channel VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 3,
    error_message TEXT,
    sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_delivery_logs_notification FOREIGN KEY (notification_id)
        REFERENCES notifications (id) ON DELETE CASCADE,
    CONSTRAINT chk_delivery_logs_channel CHECK (
        channel IN ('EMAIL', 'PUSH')
    ),
    CONSTRAINT chk_delivery_logs_status CHECK (
        status IN ('PENDING', 'SENT', 'FAILED', 'RETRYING')
    ),
    CONSTRAINT chk_delivery_logs_retry_count CHECK (
        retry_count >= 0 AND retry_count <= max_retries
    ),
    CONSTRAINT chk_delivery_logs_max_retries CHECK (max_retries >= 0),
    CONSTRAINT chk_delivery_logs_sent_at CHECK (
        sent_at IS NULL OR status = 'SENT'
    )
);

CREATE INDEX idx_delivery_logs_notification_id
    ON notification_delivery_logs (notification_id);
CREATE INDEX idx_delivery_logs_status_retry
    ON notification_delivery_logs (status, retry_count);
CREATE INDEX idx_delivery_logs_channel_status
    ON notification_delivery_logs (channel, status);
CREATE INDEX idx_delivery_logs_created_at
    ON notification_delivery_logs (created_at DESC);

CREATE TABLE device_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    fcm_token VARCHAR(500) NOT NULL,
    platform VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    invalidated_at TIMESTAMPTZ,
    CONSTRAINT uq_device_tokens_fcm_token UNIQUE (fcm_token),
    CONSTRAINT chk_device_tokens_platform CHECK (
        platform IN ('WEB', 'ANDROID', 'IOS')
    ),
    CONSTRAINT chk_device_tokens_invalidation CHECK (
        invalidated_at IS NULL OR is_active = FALSE
    )
);

CREATE INDEX idx_device_tokens_user_active
    ON device_tokens (user_id, is_active);
CREATE INDEX idx_device_tokens_user_platform
    ON device_tokens (user_id, platform);
CREATE INDEX idx_device_tokens_updated_at
    ON device_tokens (updated_at DESC);

CREATE TABLE promotion_broadcasts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    segment VARCHAR(50) NOT NULL,
    segment_ref_id UUID,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    channels JSONB NOT NULL,
    payload JSONB,
    status VARCHAR(30) NOT NULL DEFAULT 'INITIATED',
    delivered_count INTEGER NOT NULL DEFAULT 0,
    target_count INTEGER,
    created_by UUID NOT NULL,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    CONSTRAINT chk_promotion_broadcasts_channels CHECK (
        jsonb_typeof(channels) = 'array'
        AND jsonb_array_length(channels) BETWEEN 1 AND 2
    ),
    CONSTRAINT chk_promotion_broadcasts_status CHECK (
        status IN ('INITIATED', 'RESOLVING', 'COMPLETED', 'FAILED')
    ),
    CONSTRAINT chk_promotion_broadcasts_delivered_count CHECK (
        delivered_count >= 0
    ),
    CONSTRAINT chk_promotion_broadcasts_target_count CHECK (
        target_count IS NULL OR target_count >= 0
    )
);

CREATE INDEX idx_promotion_broadcasts_status_created
    ON promotion_broadcasts (status, created_at DESC);
CREATE INDEX idx_promotion_broadcasts_created_by
    ON promotion_broadcasts (created_by, created_at DESC);
