CREATE TABLE IF NOT EXISTS processed_logs (
    event_id UUID,
    ingestion_id UUID,
    application_id UUID,
    application_name String,
    application_display_name Nullable (String),
    level LowCardinality (String),
    message String,
    trace_id Nullable (String),
    log_timestamp DateTime64 (3, 'UTC'),
    received_at DateTime64 (3, 'UTC'),
    processed_at DateTime64 (3, 'UTC'),
    fingerprint Nullable (String),
    status LowCardinality (String)
) ENGINE = ReplacingMergeTree (processed_at)
PARTITION BY
    toYYYYMM (log_timestamp)
ORDER BY (
        application_id, log_timestamp, event_id
    );