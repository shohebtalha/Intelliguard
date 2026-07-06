CREATE TABLE IF NOT EXISTS app_users (
    id VARCHAR(255) PRIMARY KEY,
    tenant_id VARCHAR(80) NOT NULL DEFAULT 'demo-bank',
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL CHECK (role IN ('ANALYST', 'MANAGER', 'ADMIN')),
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS transactions (
    id VARCHAR(255) PRIMARY KEY,
    tenant_id VARCHAR(80) NOT NULL DEFAULT 'demo-bank',
    sender_id VARCHAR(255) NOT NULL,
    receiver_id VARCHAR(255) NOT NULL,
    amount NUMERIC(15, 2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(10) NOT NULL CHECK (char_length(currency) = 3),
    country VARCHAR(100) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    device_type VARCHAR(50),
    ip_address VARCHAR(50),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'APPROVE', 'REVIEW', 'BLOCK')),
    fraud_score NUMERIC(5, 4) CHECK (fraud_score IS NULL OR (fraud_score >= 0 AND fraud_score <= 1)),
    flag_reason VARCHAR(500),
    idempotency_key VARCHAR(120),
    model_version VARCHAR(80),
    created_at TIMESTAMP,
    version BIGINT
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_transactions_idempotency_key
    ON transactions (tenant_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_transactions_sender_created
    ON transactions (tenant_id, sender_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_transactions_status_created
    ON transactions (tenant_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_transactions_created_at
    ON transactions (tenant_id, created_at DESC);

CREATE TABLE IF NOT EXISTS audit_logs (
    id VARCHAR(255) PRIMARY KEY,
    tenant_id VARCHAR(80) NOT NULL DEFAULT 'demo-bank',
    transaction_id VARCHAR(255) NOT NULL,
    sender_id VARCHAR(255) NOT NULL,
    receiver_id VARCHAR(255) NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    currency VARCHAR(255) NOT NULL,
    country VARCHAR(255) NOT NULL,
    decision VARCHAR(255) NOT NULL CHECK (decision IN ('APPROVE', 'REVIEW', 'BLOCK')),
    fraud_score NUMERIC(5, 4) CHECK (fraud_score IS NULL OR (fraud_score >= 0 AND fraud_score <= 1)),
    flag_reason VARCHAR(1000),
    model_version VARCHAR(255),
    decision_time_ms BIGINT,
    performed_by VARCHAR(255),
    previous_hash VARCHAR(128),
    record_hash VARCHAR(128),
    created_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_transaction_id
    ON audit_logs (tenant_id, transaction_id);

CREATE INDEX IF NOT EXISTS idx_audit_sender_id
    ON audit_logs (tenant_id, sender_id);

CREATE INDEX IF NOT EXISTS idx_audit_created_at
    ON audit_logs (tenant_id, created_at DESC);

CREATE TABLE IF NOT EXISTS outbox_events (
    id VARCHAR(255) PRIMARY KEY,
    tenant_id VARCHAR(80) NOT NULL DEFAULT 'demo-bank',
    topic VARCHAR(255) NOT NULL,
    event_key VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(30) NOT NULL CHECK (status IN ('PENDING', 'IN_PROGRESS', 'PUBLISHED', 'FAILED')),
    attempts INTEGER NOT NULL,
    last_error VARCHAR(1000),
    created_at TIMESTAMP,
    next_attempt_at TIMESTAMP,
    published_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_outbox_status_next_attempt
    ON outbox_events (status, next_attempt_at);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id VARCHAR(255) PRIMARY KEY,
    tenant_id VARCHAR(80) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    token_family VARCHAR(80) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    replaced_by_hash VARCHAR(128),
    created_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_refresh_token_hash
    ON refresh_tokens (token_hash);

CREATE INDEX IF NOT EXISTS idx_refresh_user_family
    ON refresh_tokens (user_id, token_family);

CREATE TABLE IF NOT EXISTS fraud_cases (
    id VARCHAR(255) PRIMARY KEY,
    tenant_id VARCHAR(80) NOT NULL,
    transaction_id VARCHAR(255) NOT NULL,
    sender_id VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL CHECK (status IN ('OPEN', 'INVESTIGATING', 'RESOLVED')),
    priority VARCHAR(20) NOT NULL CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    assigned_to VARCHAR(255),
    fraud_score NUMERIC(5, 4) CHECK (fraud_score IS NULL OR (fraud_score >= 0 AND fraud_score <= 1)),
    decision VARCHAR(20) NOT NULL CHECK (decision IN ('REVIEW', 'BLOCK')),
    reason VARCHAR(1000),
    resolution VARCHAR(40),
    resolution_note VARCHAR(1000),
    resolved_by VARCHAR(255),
    resolved_at TIMESTAMP,
    created_at TIMESTAMP,
    version BIGINT,
    CONSTRAINT uk_cases_tenant_transaction UNIQUE (tenant_id, transaction_id)
);

CREATE INDEX IF NOT EXISTS idx_cases_tenant_status_priority
    ON fraud_cases (tenant_id, status, priority, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_cases_assignee_status
    ON fraud_cases (tenant_id, assigned_to, status);

CREATE TABLE IF NOT EXISTS case_notes (
    id VARCHAR(255) PRIMARY KEY,
    tenant_id VARCHAR(80) NOT NULL,
    case_id VARCHAR(255) NOT NULL,
    note VARCHAR(1000) NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_case_notes_case_created
    ON case_notes (tenant_id, case_id, created_at);

CREATE TABLE IF NOT EXISTS model_registry (
    id VARCHAR(255) PRIMARY KEY,
    version VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(40) NOT NULL CHECK (status IN ('CHAMPION', 'CHALLENGER', 'ARCHIVED')),
    artifact_path VARCHAR(500) NOT NULL,
    training_dataset VARCHAR(200),
    roc_auc NUMERIC(6, 5),
    precision_score NUMERIC(6, 5),
    recall_score NUMERIC(6, 5),
    promoted_at TIMESTAMP,
    created_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_model_registry_status
    ON model_registry (status, promoted_at DESC);

CREATE TABLE IF NOT EXISTS model_inference_metrics (
    id VARCHAR(255) PRIMARY KEY,
    tenant_id VARCHAR(80) NOT NULL,
    transaction_id VARCHAR(255),
    model_version VARCHAR(100) NOT NULL,
    fraud_probability NUMERIC(6, 5),
    latency_ms BIGINT,
    fallback BOOLEAN NOT NULL,
    error_message VARCHAR(500),
    created_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_model_metrics_version_created
    ON model_inference_metrics (model_version, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_model_metrics_tenant_created
    ON model_inference_metrics (tenant_id, created_at DESC);

CREATE TABLE IF NOT EXISTS model_drift_snapshots (
    id VARCHAR(255) PRIMARY KEY,
    model_version VARCHAR(100) NOT NULL,
    window_minutes INTEGER NOT NULL,
    sample_count BIGINT NOT NULL,
    avg_score NUMERIC(6, 5),
    fallback_rate NUMERIC(6, 5),
    high_risk_rate NUMERIC(6, 5),
    status VARCHAR(30) NOT NULL CHECK (status IN ('OK', 'WATCH', 'ALERT', 'INSUFFICIENT_DATA')),
    reason VARCHAR(500),
    created_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_drift_model_created
    ON model_drift_snapshots (model_version, created_at DESC);
