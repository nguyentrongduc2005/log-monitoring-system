CREATE TABLE alerting.chat_rooms (
    id UUID NOT NULL,
    channel VARCHAR(32) NOT NULL,
    name VARCHAR(120) NOT NULL,
    chat_id VARCHAR(128) NOT NULL,
    description TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_chat_rooms PRIMARY KEY (id),
    CONSTRAINT fk_chat_rooms_created_by
        FOREIGN KEY (created_by)
        REFERENCES identity.users (id)
        ON DELETE SET NULL,
    CONSTRAINT ck_chat_rooms_channel
        CHECK (channel IN ('TELEGRAM', 'WEBSOCKET')),
    CONSTRAINT ck_chat_rooms_name_not_blank
        CHECK (BTRIM(name) <> ''),
    CONSTRAINT ck_chat_rooms_chat_id_not_blank
        CHECK (BTRIM(chat_id) <> ''),
    CONSTRAINT ck_chat_rooms_status
        CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE UNIQUE INDEX uk_chat_rooms_channel_chat_id
    ON alerting.chat_rooms (channel, chat_id);

CREATE INDEX idx_chat_rooms_channel_status
    ON alerting.chat_rooms (channel, status);

ALTER TABLE alerting.alert_rule_channels
    ADD COLUMN chat_room_id UUID;

ALTER TABLE alerting.alert_rule_channels
    ADD CONSTRAINT fk_alert_rule_channels_chat_room
        FOREIGN KEY (chat_room_id)
        REFERENCES alerting.chat_rooms (id)
        ON DELETE RESTRICT;

ALTER TABLE alerting.alert_delivery_channels
    ADD COLUMN chat_room_id UUID;

ALTER TABLE alerting.alert_delivery_channels
    ADD CONSTRAINT fk_alert_delivery_channels_chat_room
        FOREIGN KEY (chat_room_id)
        REFERENCES alerting.chat_rooms (id)
        ON DELETE RESTRICT;
