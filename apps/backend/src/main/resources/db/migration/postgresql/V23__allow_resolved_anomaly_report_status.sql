ALTER TABLE anomaly.anomaly_reports
    DROP CONSTRAINT IF EXISTS ck_anomaly_reports_status;

ALTER TABLE anomaly.anomaly_reports
    ADD CONSTRAINT ck_anomaly_reports_status
        CHECK (status IN (
            'DETECTED',
            'ALERTED',
            'AI_PENDING',
            'AI_SUCCEEDED',
            'AI_FAILED',
            'RESOLVED'
        ));
