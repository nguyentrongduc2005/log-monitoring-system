CREATE TABLE incident_anomaly_reports (
    id UUID PRIMARY KEY,
    alert_id UUID NOT NULL,
    evidence_payload JSONB NOT NULL,
    ai_analysis_result TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_incident_anomaly_reports_alert_id ON incident_anomaly_reports(alert_id);
