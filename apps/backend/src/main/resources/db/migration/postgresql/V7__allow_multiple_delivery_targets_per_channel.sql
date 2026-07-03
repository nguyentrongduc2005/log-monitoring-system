ALTER TABLE alerting.alert_rule_channels
    DROP CONSTRAINT pk_alert_rule_channels,
    ADD COLUMN id UUID NOT NULL DEFAULT gen_random_uuid(),
    ADD CONSTRAINT pk_alert_rule_channels PRIMARY KEY (id);

CREATE UNIQUE INDEX uk_alert_rule_delivery_target
    ON alerting.alert_rule_channels (
        rule_id,
        channel,
        COALESCE(chat_room_id, '00000000-0000-0000-0000-000000000000'::UUID)
    );

ALTER TABLE alerting.alert_delivery_channels
    DROP CONSTRAINT pk_alert_delivery_channels,
    ADD COLUMN id UUID NOT NULL DEFAULT gen_random_uuid(),
    ADD CONSTRAINT pk_alert_delivery_channels PRIMARY KEY (id);

CREATE UNIQUE INDEX uk_alert_delivery_target
    ON alerting.alert_delivery_channels (
        alert_id,
        channel,
        COALESCE(chat_room_id, '00000000-0000-0000-0000-000000000000'::UUID)
    );
