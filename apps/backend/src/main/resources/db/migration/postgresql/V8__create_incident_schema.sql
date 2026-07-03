CREATE SCHEMA IF NOT EXISTS incident;

CREATE TABLE incident.incidents (
    id UUID NOT NULL,
    title VARCHAR(180) NOT NULL,
    description TEXT,
    status VARCHAR(32) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    scope VARCHAR(32) NOT NULL,
    trigger_type VARCHAR(32) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    window_start TIMESTAMPTZ NOT NULL,
    window_end TIMESTAMPTZ NOT NULL,
    created_by UUID NOT NULL,
    resolved_by UUID,
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_incidents PRIMARY KEY (id),
    CONSTRAINT fk_incidents_created_by
        FOREIGN KEY (created_by)
        REFERENCES identity.users (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_incidents_resolved_by
        FOREIGN KEY (resolved_by)
        REFERENCES identity.users (id)
        ON DELETE SET NULL,
    CONSTRAINT ck_incidents_title_not_blank
        CHECK (BTRIM(title) <> ''),
    CONSTRAINT ck_incidents_status
        CHECK (status IN ('INVESTIGATING', 'MITIGATED', 'RESOLVED')),
    CONSTRAINT ck_incidents_severity
        CHECK (severity IN ('SEV1', 'SEV2', 'SEV3', 'UNKNOWN')),
    CONSTRAINT ck_incidents_scope
        CHECK (scope IN ('APPLICATION', 'MULTI_APPLICATION', 'SYSTEM_WIDE')),
    CONSTRAINT ck_incidents_trigger_type
        CHECK (trigger_type IN ('MANUAL')),
    CONSTRAINT ck_incidents_window_order
        CHECK (window_start <= window_end),
    CONSTRAINT ck_incidents_resolved_state
        CHECK (
            (status = 'RESOLVED' AND resolved_at IS NOT NULL)
            OR (status <> 'RESOLVED')
        )
);

CREATE INDEX idx_incidents_status_severity_started_at
    ON incident.incidents (status, severity, started_at DESC);

CREATE INDEX idx_incidents_created_by_started_at
    ON incident.incidents (created_by, started_at DESC);

CREATE INDEX idx_incidents_window
    ON incident.incidents (window_start, window_end);

CREATE TABLE incident.incident_applications (
    incident_id UUID NOT NULL,
    application_id UUID NOT NULL,
    impact_role VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_incident_applications PRIMARY KEY (incident_id, application_id),
    CONSTRAINT fk_incident_applications_incident
        FOREIGN KEY (incident_id)
        REFERENCES incident.incidents (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_incident_applications_application
        FOREIGN KEY (application_id)
        REFERENCES identity.applications (id)
        ON DELETE CASCADE,
    CONSTRAINT ck_incident_applications_impact_role
        CHECK (impact_role IN ('PRIMARY', 'RELATED', 'SUSPECTED'))
);

CREATE INDEX idx_incident_applications_application
    ON incident.incident_applications (application_id, incident_id);

CREATE TABLE incident.incident_alerts (
    incident_id UUID NOT NULL,
    alert_id UUID NOT NULL,
    relation_type VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_incident_alerts PRIMARY KEY (incident_id, alert_id),
    CONSTRAINT fk_incident_alerts_incident
        FOREIGN KEY (incident_id)
        REFERENCES incident.incidents (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_incident_alerts_alert
        FOREIGN KEY (alert_id)
        REFERENCES alerting.alerts (id)
        ON DELETE CASCADE,
    CONSTRAINT ck_incident_alerts_relation_type
        CHECK (relation_type IN ('TRIGGER', 'RELATED', 'EVIDENCE'))
);

CREATE INDEX idx_incident_alerts_alert
    ON incident.incident_alerts (alert_id);

CREATE TABLE incident.incident_evidence (
    id UUID NOT NULL,
    incident_id UUID NOT NULL,
    type VARCHAR(32) NOT NULL,
    source_id VARCHAR(128),
    application_id UUID,
    fingerprint VARCHAR(128),
    trace_id VARCHAR(128),
    severity VARCHAR(32),
    summary TEXT NOT NULL,
    sample_message TEXT,
    occurred_at TIMESTAMPTZ,
    metadata_json TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_incident_evidence PRIMARY KEY (id),
    CONSTRAINT fk_incident_evidence_incident
        FOREIGN KEY (incident_id)
        REFERENCES incident.incidents (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_incident_evidence_application
        FOREIGN KEY (application_id)
        REFERENCES identity.applications (id)
        ON DELETE SET NULL,
    CONSTRAINT ck_incident_evidence_type
        CHECK (type IN ('ALERT', 'LOG', 'TRACE', 'HEALTH', 'DEPLOYMENT')),
    CONSTRAINT ck_incident_evidence_summary_not_blank
        CHECK (BTRIM(summary) <> '')
);

CREATE INDEX idx_incident_evidence_incident_type
    ON incident.incident_evidence (incident_id, type);

CREATE INDEX idx_incident_evidence_application_occurred_at
    ON incident.incident_evidence (application_id, occurred_at DESC);

CREATE INDEX idx_incident_evidence_fingerprint
    ON incident.incident_evidence (fingerprint);

CREATE INDEX idx_incident_evidence_trace_id
    ON incident.incident_evidence (trace_id);

CREATE TABLE incident.incident_ai_analyses (
    id UUID NOT NULL,
    incident_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    provider VARCHAR(64),
    model VARCHAR(120),
    prompt_version VARCHAR(32) NOT NULL,
    summary TEXT,
    likely_cause TEXT,
    severity VARCHAR(32),
    severity_reason TEXT,
    confidence VARCHAR(32),
    suggested_actions_json TEXT,
    evidence_refs_json TEXT,
    raw_response_json TEXT,
    error_message TEXT,
    requested_by UUID NOT NULL,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_incident_ai_analyses PRIMARY KEY (id),
    CONSTRAINT fk_incident_ai_analyses_incident
        FOREIGN KEY (incident_id)
        REFERENCES incident.incidents (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_incident_ai_analyses_requested_by
        FOREIGN KEY (requested_by)
        REFERENCES identity.users (id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_incident_ai_analyses_status
        CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED')),
    CONSTRAINT ck_incident_ai_analyses_confidence
        CHECK (confidence IS NULL OR confidence IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT ck_incident_ai_analyses_completed_order
        CHECK (completed_at IS NULL OR started_at IS NULL OR completed_at >= started_at)
);

CREATE INDEX idx_incident_ai_analyses_incident_created_at
    ON incident.incident_ai_analyses (incident_id, created_at DESC);

CREATE INDEX idx_incident_ai_analyses_status_created_at
    ON incident.incident_ai_analyses (status, created_at);

CREATE TABLE incident.incident_timeline_events (
    id UUID NOT NULL,
    incident_id UUID NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    message TEXT NOT NULL,
    actor_user_id UUID,
    metadata_json TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_incident_timeline_events PRIMARY KEY (id),
    CONSTRAINT fk_incident_timeline_events_incident
        FOREIGN KEY (incident_id)
        REFERENCES incident.incidents (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_incident_timeline_events_actor
        FOREIGN KEY (actor_user_id)
        REFERENCES identity.users (id)
        ON DELETE SET NULL,
    CONSTRAINT ck_incident_timeline_events_message_not_blank
        CHECK (BTRIM(message) <> '')
);

CREATE INDEX idx_incident_timeline_events_incident_created_at
    ON incident.incident_timeline_events (incident_id, created_at ASC);
