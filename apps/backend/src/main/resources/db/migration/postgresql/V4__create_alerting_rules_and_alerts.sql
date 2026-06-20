CREATE SCHEMA IF NOT EXISTS alerting;

CREATE TABLE alerting.alert_rules (
    id UUID NOT NULL,
    application_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    min_severity VARCHAR(32) NOT NULL,
    keyword_pattern VARCHAR(255),
    threshold_count INTEGER NOT NULL,
    threshold_window_seconds INTEGER NOT NULL,
    cooldown_seconds INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_alert_rules PRIMARY KEY (id),
    CONSTRAINT fk_alert_rules_application
        FOREIGN KEY (application_id)
        REFERENCES identity.applications (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_alert_rules_created_by
        FOREIGN KEY (created_by)
        REFERENCES identity.users (id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_alert_rules_name_not_blank
        CHECK (BTRIM(name) <> ''),
    CONSTRAINT ck_alert_rules_min_severity
        CHECK (min_severity IN ('INFO', 'WARN', 'ERROR', 'CRITICAL')),
    CONSTRAINT ck_alert_rules_threshold_count_positive
        CHECK (threshold_count > 0),
    CONSTRAINT ck_alert_rules_threshold_window_positive
        CHECK (threshold_window_seconds > 0),
    CONSTRAINT ck_alert_rules_cooldown_positive
        CHECK (cooldown_seconds > 0),
    CONSTRAINT ck_alert_rules_status
        CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE UNIQUE INDEX uk_alert_rules_application_name
    ON alerting.alert_rules (application_id, LOWER(name));

CREATE INDEX idx_alert_rules_application_status
    ON alerting.alert_rules (application_id, status);

CREATE INDEX idx_alert_rules_min_severity
    ON alerting.alert_rules (min_severity);

CREATE TABLE alerting.alert_rule_channels (
    rule_id UUID NOT NULL,
    channel VARCHAR(32) NOT NULL,

    CONSTRAINT pk_alert_rule_channels PRIMARY KEY (rule_id, channel),
    CONSTRAINT fk_alert_rule_channels_rule
        FOREIGN KEY (rule_id)
        REFERENCES alerting.alert_rules (id)
        ON DELETE CASCADE,
    CONSTRAINT ck_alert_rule_channels_channel
        CHECK (channel IN ('TELEGRAM', 'WEBSOCKET'))
);

CREATE TABLE alerting.alerts (
    id UUID NOT NULL,
    rule_id UUID NOT NULL,
    application_id UUID NOT NULL,
    event_id UUID NOT NULL,
    ingestion_id UUID NOT NULL,
    application_name VARCHAR(100) NOT NULL,
    application_display_name VARCHAR(150),
    severity VARCHAR(32) NOT NULL,
    message TEXT NOT NULL,
    fingerprint VARCHAR(128) NOT NULL,
    log_timestamp TIMESTAMPTZ NOT NULL,
    triggered_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    acknowledged_by UUID,
    acknowledged_at TIMESTAMPTZ,
    resolved_by UUID,
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_alerts PRIMARY KEY (id),
    CONSTRAINT fk_alerts_rule
        FOREIGN KEY (rule_id)
        REFERENCES alerting.alert_rules (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_alerts_application
        FOREIGN KEY (application_id)
        REFERENCES identity.applications (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_alerts_acknowledged_by
        FOREIGN KEY (acknowledged_by)
        REFERENCES identity.users (id)
        ON DELETE SET NULL,
    CONSTRAINT fk_alerts_resolved_by
        FOREIGN KEY (resolved_by)
        REFERENCES identity.users (id)
        ON DELETE SET NULL,
    CONSTRAINT ck_alerts_application_name_not_blank
        CHECK (BTRIM(application_name) <> ''),
    CONSTRAINT ck_alerts_message_not_blank
        CHECK (BTRIM(message) <> ''),
    CONSTRAINT ck_alerts_fingerprint_not_blank
        CHECK (BTRIM(fingerprint) <> ''),
    CONSTRAINT ck_alerts_severity
        CHECK (severity IN ('INFO', 'WARN', 'ERROR', 'CRITICAL')),
    CONSTRAINT ck_alerts_status
        CHECK (status IN ('OPEN', 'ACKNOWLEDGED', 'RESOLVED'))
);

CREATE INDEX idx_alerts_application_status
    ON alerting.alerts (application_id, status);

CREATE INDEX idx_alerts_rule_triggered_at
    ON alerting.alerts (rule_id, triggered_at DESC);

CREATE INDEX idx_alerts_fingerprint
    ON alerting.alerts (fingerprint);

CREATE TABLE alerting.alert_delivery_channels (
    alert_id UUID NOT NULL,
    channel VARCHAR(32) NOT NULL,

    CONSTRAINT pk_alert_delivery_channels PRIMARY KEY (alert_id, channel),
    CONSTRAINT fk_alert_delivery_channels_alert
        FOREIGN KEY (alert_id)
        REFERENCES alerting.alerts (id)
        ON DELETE CASCADE,
    CONSTRAINT ck_alert_delivery_channels_channel
        CHECK (channel IN ('TELEGRAM', 'WEBSOCKET'))
);
