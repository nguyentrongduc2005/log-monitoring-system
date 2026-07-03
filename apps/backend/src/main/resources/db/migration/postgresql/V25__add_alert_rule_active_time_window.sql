ALTER TABLE alerting.alert_rules
    ADD COLUMN active_start_time TIME,
    ADD COLUMN active_end_time TIME;

ALTER TABLE alerting.alert_rules
    ADD CONSTRAINT ck_alert_rules_active_time_window
        CHECK (
            (active_start_time IS NULL AND active_end_time IS NULL)
            OR (
                active_start_time IS NOT NULL
                AND active_end_time IS NOT NULL
                AND active_start_time <> active_end_time
            )
        );
