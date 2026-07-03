CREATE SCHEMA IF NOT EXISTS anomaly;

CREATE TABLE anomaly.anomaly_reports (
    id UUID NOT NULL PRIMARY KEY,
    application_id UUID NOT NULL,
    alert_id UUID,
    source_type VARCHAR(32) NOT NULL,
    rule_name VARCHAR(120) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    title VARCHAR(180) NOT NULL,
    summary TEXT,
    hypothesis TEXT,
    confidence_score DOUBLE PRECISION,
    likelihood_label VARCHAR(32),
    impact_summary TEXT,
    investigation_steps JSONB,
    recommended_actions JSONB,
    dimension_type VARCHAR(64),
    dimension_value VARCHAR(255),
    metric_group VARCHAR(120),
    observed_value DOUBLE PRECISION,
    threshold_value DOUBLE PRECISION,
    observed_count BIGINT,
    threshold_count BIGINT,
    window_start TIMESTAMPTZ NOT NULL,
    window_end TIMESTAMPTZ NOT NULL,
    evidence_payload JSONB NOT NULL,
    ai_trigger_requested BOOLEAN NOT NULL DEFAULT FALSE,
    ai_trigger_reason VARCHAR(120),
    ai_status VARCHAR(32) NOT NULL DEFAULT 'NOT_REQUESTED',
    ai_model VARCHAR(120),
    ai_prompt_version VARCHAR(64),
    ai_started_at TIMESTAMPTZ,
    ai_completed_at TIMESTAMPTZ,
    ai_summary TEXT,
    ai_confidence_score DOUBLE PRECISION,
    ai_likelihood_label VARCHAR(32),
    ai_root_cause_candidates JSONB,
    ai_recommended_actions JSONB,
    ai_investigation_steps JSONB,
    ai_result JSONB,
    ai_raw_response JSONB,
    ai_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_anomaly_reports_application
        FOREIGN KEY (application_id)
        REFERENCES identity.applications (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_anomaly_reports_alert
        FOREIGN KEY (alert_id)
        REFERENCES alerting.alerts (id)
        ON DELETE SET NULL,
    CONSTRAINT ck_anomaly_reports_source_type
        CHECK (source_type IN ('ANOMALY_LOG', 'ANOMALY_METRIC')),
    CONSTRAINT ck_anomaly_reports_severity
        CHECK (severity IN ('WARN', 'WARNING', 'ERROR', 'CRITICAL')),
    CONSTRAINT ck_anomaly_reports_status
        CHECK (status IN ('DETECTED', 'ALERTED', 'AI_PENDING', 'AI_SUCCEEDED', 'AI_FAILED')),
    CONSTRAINT ck_anomaly_reports_ai_status
        CHECK (ai_status IN ('NOT_REQUESTED', 'PENDING', 'SUCCEEDED', 'FAILED')),
    CONSTRAINT ck_anomaly_reports_confidence_score
        CHECK (confidence_score IS NULL OR (confidence_score >= 0 AND confidence_score <= 1)),
    CONSTRAINT ck_anomaly_reports_ai_confidence_score
        CHECK (ai_confidence_score IS NULL OR (ai_confidence_score >= 0 AND ai_confidence_score <= 1)),
    CONSTRAINT ck_anomaly_reports_window
        CHECK (window_start <= window_end)
);

CREATE INDEX idx_anomaly_reports_application_created_at
    ON anomaly.anomaly_reports (application_id, created_at DESC);

CREATE INDEX idx_anomaly_reports_source_rule_window
    ON anomaly.anomaly_reports (source_type, rule_name, window_start, window_end);

CREATE INDEX idx_anomaly_reports_dimension
    ON anomaly.anomaly_reports (application_id, rule_name, dimension_type, dimension_value);

CREATE INDEX idx_anomaly_reports_alert_id
    ON anomaly.anomaly_reports (alert_id);
