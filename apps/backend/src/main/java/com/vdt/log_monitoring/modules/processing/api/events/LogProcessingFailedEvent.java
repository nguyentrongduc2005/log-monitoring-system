package com.vdt.log_monitoring.modules.processing.api.events;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record LogProcessingFailedEvent(
        UUID eventId,
        UUID ingestionId,
        UUID applicationId,
        String applicationName,
        String failureCode,
        String failureMessage,
        Instant failedAt,
        int schemaVersion) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public LogProcessingFailedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(ingestionId, "ingestionId must not be null");
        Objects.requireNonNull(applicationId, "applicationId must not be null");
        Objects.requireNonNull(applicationName, "applicationName must not be null");
        Objects.requireNonNull(failureCode, "failureCode must not be null");
        Objects.requireNonNull(failureMessage, "failureMessage must not be null");
        Objects.requireNonNull(failedAt, "failedAt must not be null");

        if (applicationName.isBlank()) {
            throw new IllegalArgumentException("applicationName must not be blank");
        }

        if (failureCode.isBlank()) {
            throw new IllegalArgumentException("failureCode must not be blank");
        }

        if (schemaVersion <= 0) {
            throw new IllegalArgumentException("schemaVersion must be positive");
        }
    }
}
