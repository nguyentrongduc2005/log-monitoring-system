ALTER TABLE incident.incidents
    ADD COLUMN last_evidence_collected_at TIMESTAMPTZ;

CREATE INDEX idx_incidents_last_evidence_collected_at
    ON incident.incidents (last_evidence_collected_at);
