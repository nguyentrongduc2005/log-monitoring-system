ALTER TABLE alerting.alert_rules ADD COLUMN severity VARCHAR(32);

UPDATE alerting.alert_rules SET severity = min_severity;

ALTER TABLE alerting.alert_rules ALTER COLUMN severity SET NOT NULL;

ALTER TABLE alerting.alert_rules ADD CONSTRAINT ck_alert_rules_severity CHECK (severity IN ('INFO', 'WARN', 'ERROR', 'CRITICAL'));
