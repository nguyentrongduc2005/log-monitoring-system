CREATE TABLE identity.applications (
    id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    display_name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_applications PRIMARY KEY (id),
    CONSTRAINT fk_applications_created_by
        FOREIGN KEY (created_by)
        REFERENCES identity.users (id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_applications_name_not_blank
        CHECK (BTRIM(name) <> ''),
    CONSTRAINT ck_applications_display_name_not_blank
        CHECK (BTRIM(display_name) <> ''),
    CONSTRAINT ck_applications_status
        CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE UNIQUE INDEX uk_applications_name
    ON identity.applications (LOWER(name));

CREATE INDEX idx_applications_status
    ON identity.applications (status);

CREATE TABLE identity.user_application_access (
    user_id UUID NOT NULL,
    application_id UUID NOT NULL,
    access_level VARCHAR(32) NOT NULL,
    granted_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_user_application_access PRIMARY KEY (user_id, application_id),
    CONSTRAINT fk_user_application_access_user
        FOREIGN KEY (user_id)
        REFERENCES identity.users (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_user_application_access_application
        FOREIGN KEY (application_id)
        REFERENCES identity.applications (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_user_application_access_granted_by
        FOREIGN KEY (granted_by)
        REFERENCES identity.users (id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_user_application_access_level
        CHECK (access_level IN ('VIEW', 'MANAGE'))
);

CREATE INDEX idx_user_application_access_application_user
    ON identity.user_application_access (application_id, user_id);

CREATE TABLE identity.application_api_keys (
    id UUID NOT NULL,
    application_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    key_prefix VARCHAR(32) NOT NULL,
    key_hash VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    expires_at TIMESTAMPTZ,
    last_used_at TIMESTAMPTZ,
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMPTZ,

    CONSTRAINT pk_application_api_keys PRIMARY KEY (id),
    CONSTRAINT fk_application_api_keys_application
        FOREIGN KEY (application_id)
        REFERENCES identity.applications (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_application_api_keys_created_by
        FOREIGN KEY (created_by)
        REFERENCES identity.users (id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_application_api_keys_name_not_blank
        CHECK (BTRIM(name) <> ''),
    CONSTRAINT ck_application_api_keys_prefix_not_blank
        CHECK (BTRIM(key_prefix) <> ''),
    CONSTRAINT ck_application_api_keys_hash_not_blank
        CHECK (BTRIM(key_hash) <> ''),
    CONSTRAINT ck_application_api_keys_status
        CHECK (status IN ('ACTIVE', 'REVOKED', 'EXPIRED'))
);

CREATE UNIQUE INDEX uk_application_api_keys_prefix
    ON identity.application_api_keys (key_prefix);

CREATE INDEX idx_application_api_keys_application_status
    ON identity.application_api_keys (application_id, status);
