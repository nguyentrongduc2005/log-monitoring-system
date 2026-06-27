ALTER TABLE alerting.alerts
ADD COLUMN rule_name VARCHAR(255) DEFAULT 'Unknown Rule' NOT NULL;
