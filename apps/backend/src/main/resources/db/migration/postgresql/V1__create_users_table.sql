CREATE TABLE users (
    id UUID NOT NULL,
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(150) NOT NULL,
    role VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT ck_users_email_not_blank
        CHECK (BTRIM(email) <> ''),
    CONSTRAINT ck_users_password_hash_not_blank
        CHECK (BTRIM(password_hash) <> ''),
    CONSTRAINT ck_users_display_name_not_blank
        CHECK (BTRIM(display_name) <> ''),
    CONSTRAINT ck_users_role
        CHECK (role IN ('ADMIN', 'ENGINEER')),
    CONSTRAINT ck_users_status
        CHECK (status IN ('ACTIVE', 'DISABLED', 'LOCKED'))
);

CREATE UNIQUE INDEX uk_users_email
    ON users (LOWER(email));

CREATE INDEX idx_users_status
    ON users (status);
