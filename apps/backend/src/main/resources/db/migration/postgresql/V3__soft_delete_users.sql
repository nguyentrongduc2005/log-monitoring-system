ALTER TABLE identity.users
    ADD COLUMN deleted_at TIMESTAMPTZ;

ALTER TABLE identity.users
    DROP CONSTRAINT ck_users_status;

ALTER TABLE identity.users
    ADD CONSTRAINT ck_users_status
        CHECK (status IN ('ACTIVE', 'DISABLED', 'LOCKED', 'DELETED'));

CREATE INDEX idx_users_deleted_at
    ON identity.users (deleted_at);
