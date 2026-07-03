-- Drop constraints for columns to be removed
ALTER TABLE alerting.alerts
    DROP CONSTRAINT ck_alerts_message_not_blank,
    DROP CONSTRAINT ck_alerts_fingerprint_not_blank;

-- Drop indices that depend on fingerprint
DROP INDEX IF EXISTS alerting.idx_alerts_fingerprint;
DROP INDEX IF EXISTS alerting.idx_alerts_application_fingerprint_last_seen;

-- Drop columns that are no longer needed
ALTER TABLE alerting.alerts
    DROP COLUMN event_id,
    DROP COLUMN ingestion_id,
    DROP COLUMN message,
    DROP COLUMN fingerprint,
    DROP COLUMN log_timestamp;

-- Add new log_samples column
ALTER TABLE alerting.alerts
    ADD COLUMN log_samples text[] NOT NULL DEFAULT ARRAY[]::text[];

-- Create new index to replace the old last_seen index (without fingerprint)
CREATE INDEX idx_alerts_application_last_seen
    ON alerting.alerts (application_id, last_seen_at DESC);
