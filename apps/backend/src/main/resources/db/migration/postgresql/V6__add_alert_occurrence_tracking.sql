ALTER TABLE alerting.alerts
    ADD COLUMN occurrence_count BIGINT NOT NULL DEFAULT 1,
    ADD COLUMN first_seen_at TIMESTAMPTZ,
    ADD COLUMN last_seen_at TIMESTAMPTZ;

UPDATE alerting.alerts
SET first_seen_at = log_timestamp,
    last_seen_at = log_timestamp
WHERE first_seen_at IS NULL OR last_seen_at IS NULL;

ALTER TABLE alerting.alerts
    ALTER COLUMN first_seen_at SET NOT NULL,
    ALTER COLUMN last_seen_at SET NOT NULL,
    ADD CONSTRAINT ck_alerts_occurrence_count_positive CHECK (occurrence_count >= 1);

CREATE INDEX idx_alerts_application_fingerprint_last_seen
    ON alerting.alerts (application_id, fingerprint, last_seen_at DESC);
