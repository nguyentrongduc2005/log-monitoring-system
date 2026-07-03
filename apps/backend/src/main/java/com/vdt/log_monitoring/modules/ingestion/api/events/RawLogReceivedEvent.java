package com.vdt.log_monitoring.modules.ingestion.api.events;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record RawLogReceivedEvent(
        UUID eventId,
        UUID ingestionId,
        UUID applicationId,
        String applicationName,
        String applicationDisplayName,
        String rawLog,
        Instant receivedAt,
        int schemaVersion) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public RawLogReceivedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(ingestionId, "ingestionId must not be null");
        Objects.requireNonNull(applicationId, "applicationId must not be null");
        Objects.requireNonNull(applicationName, "applicationName must not be null");
        Objects.requireNonNull(rawLog, "rawLog must not be null");
        Objects.requireNonNull(receivedAt, "receivedAt must not be null");

        if (applicationName.isBlank()) {
            throw new IllegalArgumentException("applicationName must not be blank");
        }

        if (rawLog.isBlank()) {
            throw new IllegalArgumentException("rawLog must not be blank");
        }

        if (schemaVersion <= 0) {
            throw new IllegalArgumentException("schemaVersion must be positive");
        }

    }
}
