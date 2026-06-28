ALTER TABLE alerting.alerts
    ALTER COLUMN rule_id DROP NOT NULL,
    ADD COLUMN trigger_type VARCHAR(32) NOT NULL DEFAULT 'LOG_RULE',
    ADD COLUMN source_type VARCHAR(32),
    ADD COLUMN source_id UUID,
    ADD COLUMN summary TEXT,
    ADD COLUMN metadata_json JSONB;

ALTER TABLE alerting.alerts
    ADD CONSTRAINT ck_alerts_trigger_type
    CHECK (trigger_type IN ('LOG_RULE', 'ANOMALY_LOG', 'ANOMALY_METRIC'));

ALTER TABLE alerting.alerts
    ADD CONSTRAINT ck_alerts_source_type
    CHECK (source_type IS NULL OR source_type IN ('LOG', 'METRIC', 'ANOMALY_REPORT'));

CREATE INDEX idx_alerts_trigger_source
    ON alerting.alerts (trigger_type, source_id);
