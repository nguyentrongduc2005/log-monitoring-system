CREATE SCHEMA IF NOT EXISTS retention;

CREATE TABLE retention.retention_policies (
    id UUID PRIMARY KEY,
    log_level VARCHAR(32) NOT NULL UNIQUE,
    label VARCHAR(80) NOT NULL,
    description TEXT,
    retention_days INTEGER NOT NULL,
    min_days INTEGER NOT NULL,
    max_days INTEGER NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_retention_days_range CHECK (
        retention_days >= min_days
        AND retention_days <= max_days
    ),
    CONSTRAINT ck_retention_log_level CHECK (
        log_level IN ('INFO', 'WARN', 'ERROR', 'CRITICAL')
    )
);

CREATE TABLE retention.retention_runs (
    id UUID PRIMARY KEY,
    policy_id UUID NOT NULL REFERENCES retention.retention_policies(id),
    status VARCHAR(32) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ,
    affected_rows BIGINT NOT NULL DEFAULT 0,
    message TEXT,
    CONSTRAINT ck_retention_run_status CHECK (
        status IN ('SUCCESS', 'FAILED')
    )
);

CREATE INDEX idx_retention_runs_policy_started
    ON retention.retention_runs(policy_id, started_at DESC);

INSERT INTO retention.retention_policies (
    id,
    log_level,
    label,
    description,
    retention_days,
    min_days,
    max_days,
    sort_order
) VALUES
    (
        '00000000-0000-0000-0000-000000000501',
        'INFO',
        'INFO logs',
        'Routine application logs and request traces.',
        7,
        1,
        365,
        10
    ),
    (
        '00000000-0000-0000-0000-000000000502',
        'WARN',
        'WARN logs',
        'Potentially degraded behavior that still needs short-term review.',
        30,
        7,
        365,
        20
    ),
    (
        '00000000-0000-0000-0000-000000000503',
        'ERROR',
        'ERROR logs',
        'Application errors retained longer for incident investigation.',
        90,
        14,
        730,
        30
    ),
    (
        '00000000-0000-0000-0000-000000000504',
        'CRITICAL',
        'CRITICAL logs',
        'High-severity failures kept longest for root cause analysis.',
        180,
        30,
        730,
        40
    )
ON CONFLICT (log_level) DO NOTHING;
