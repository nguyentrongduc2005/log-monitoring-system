ALTER TABLE anomaly.anomaly_reports
    ADD COLUMN IF NOT EXISTS fingerprint VARCHAR(255),
    ADD COLUMN IF NOT EXISTS occurrence_count BIGINT NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS first_seen_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS last_seen_at TIMESTAMPTZ;

UPDATE anomaly.anomaly_reports
SET fingerprint = COALESCE(fingerprint, id::text),
    first_seen_at = COALESCE(first_seen_at, window_start, created_at),
    last_seen_at = COALESCE(last_seen_at, window_end, updated_at)
WHERE fingerprint IS NULL
   OR first_seen_at IS NULL
   OR last_seen_at IS NULL;

ALTER TABLE anomaly.anomaly_reports
    ALTER COLUMN fingerprint SET NOT NULL,
    ALTER COLUMN first_seen_at SET NOT NULL,
    ALTER COLUMN last_seen_at SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_anomaly_reports_open_identity
    ON anomaly.anomaly_reports(application_id, source_type, rule_name, fingerprint, status);
