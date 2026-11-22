CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    timezone VARCHAR(60) NOT NULL DEFAULT 'UTC',
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_users_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE TABLE merchants (
    id BIGSERIAL PRIMARY KEY,
    owner_user_id BIGINT REFERENCES users(id),
    name VARCHAR(150) NOT NULL,
    category VARCHAR(50),
    website_url VARCHAR(255),
    logo_url TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX uq_global_merchant_name ON merchants (LOWER(name)) WHERE owner_user_id IS NULL;
CREATE UNIQUE INDEX uq_custom_merchant_name ON merchants (owner_user_id, LOWER(name)) WHERE owner_user_id IS NOT NULL;
CREATE INDEX idx_merchants_owner ON merchants(owner_user_id);

CREATE TABLE subscription_plans (
    id BIGSERIAL PRIMARY KEY,
    merchant_id BIGINT NOT NULL REFERENCES merchants(id),
    plan_name VARCHAR(100) NOT NULL,
    billing_cycle VARCHAR(20) NOT NULL,
    default_price NUMERIC(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_plans_cycle CHECK (billing_cycle IN ('WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY')),
    CONSTRAINT chk_plans_price CHECK (default_price >= 0),
    CONSTRAINT chk_plans_currency CHECK (currency ~ '^[A-Z]{3}$')
);
CREATE INDEX idx_subscription_plans_merchant ON subscription_plans(merchant_id);

CREATE TABLE user_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    merchant_id BIGINT NOT NULL REFERENCES merchants(id),
    plan_id BIGINT REFERENCES subscription_plans(id),
    custom_plan_name VARCHAR(100),
    amount NUMERIC(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    billing_cycle VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    next_billing_date DATE NOT NULL,
    end_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    reminder_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_subscriptions_amount CHECK (amount >= 0),
    CONSTRAINT chk_subscriptions_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT chk_subscriptions_cycle CHECK (billing_cycle IN ('WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY')),
    CONSTRAINT chk_subscriptions_status CHECK (status IN ('ACTIVE', 'PAUSED', 'CANCELLED', 'EXPIRED'))
);
CREATE INDEX idx_user_subscriptions_user ON user_subscriptions(user_id);
CREATE INDEX idx_user_subscriptions_merchant ON user_subscriptions(merchant_id);
CREATE INDEX idx_user_subscriptions_next_billing ON user_subscriptions(next_billing_date);
CREATE INDEX idx_user_subscriptions_status ON user_subscriptions(status);

CREATE TABLE payment_history (
    id BIGSERIAL PRIMARY KEY,
    subscription_id BIGINT NOT NULL REFERENCES user_subscriptions(id),
    amount NUMERIC(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    payment_date DATE NOT NULL,
    payment_status VARCHAR(20) NOT NULL,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_payments_amount CHECK (amount >= 0),
    CONSTRAINT chk_payments_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT chk_payments_status CHECK (payment_status IN ('PAID', 'PENDING', 'FAILED'))
);
CREATE INDEX idx_payment_history_subscription ON payment_history(subscription_id);
CREATE INDEX idx_payment_history_date ON payment_history(payment_date);

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    subscription_id BIGINT REFERENCES user_subscriptions(id),
    notification_type VARCHAR(30) NOT NULL,
    scheduled_at TIMESTAMPTZ NOT NULL,
    sent_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    claimed_at TIMESTAMPTZ,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_notifications_status CHECK (status IN ('PENDING', 'CLAIMED', 'SENT', 'FAILED')),
    CONSTRAINT chk_notifications_attempts CHECK (attempt_count >= 0)
);
CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_scheduling ON notifications(status, scheduled_at);
CREATE UNIQUE INDEX uq_subscription_reminder ON notifications(subscription_id, notification_type, scheduled_at)
    WHERE subscription_id IS NOT NULL;

INSERT INTO merchants (name, category, website_url, is_active) VALUES
('Netflix', 'Streaming', 'https://www.netflix.com', TRUE),
('Spotify', 'Music', 'https://www.spotify.com', TRUE),
('Amazon Prime', 'Shopping & Streaming', 'https://www.amazon.com/prime', TRUE),
('YouTube Premium', 'Streaming', 'https://www.youtube.com/premium', TRUE),
('Microsoft 365', 'Productivity', 'https://www.microsoft.com/microsoft-365', TRUE);
