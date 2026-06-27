ALTER TABLE alerting.alerts
    ALTER COLUMN log_samples DROP DEFAULT,
    ALTER COLUMN log_samples TYPE jsonb USING '[]'::jsonb,
    ALTER COLUMN log_samples SET DEFAULT '[]'::jsonb;
