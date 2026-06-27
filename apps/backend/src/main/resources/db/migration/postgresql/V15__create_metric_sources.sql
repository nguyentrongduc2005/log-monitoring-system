CREATE TABLE identity.metric_sources (
    id UUID PRIMARY KEY,
    application_id UUID NOT NULL UNIQUE REFERENCES identity.applications(id),
    target_host VARCHAR(255) NOT NULL,
    target_port INT NOT NULL,
    metrics_path VARCHAR(255) NOT NULL DEFAULT '/actuator/prometheus',
    scrape_interval VARCHAR(32) NOT NULL DEFAULT '15s',
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
