ALTER TABLE incident.incidents
    DROP CONSTRAINT ck_incidents_trigger_type;

ALTER TABLE incident.incidents
    ADD CONSTRAINT ck_incidents_trigger_type
    CHECK (trigger_type IN ('MANUAL', 'ALERT'));
