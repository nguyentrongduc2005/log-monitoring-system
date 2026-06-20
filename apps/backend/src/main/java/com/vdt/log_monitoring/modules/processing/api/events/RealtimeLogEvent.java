package com.vdt.log_monitoring.modules.processing.api.events;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record RealtimeLogEvent(
        UUID eventId,
        UUID ingestionId,
        UUID applicationId,
        String applicationName,
        String applicationDisplayName,
        String level,
        String message,
        String traceId,
        Instant logTimestamp,
        Instant processedAt,
        int schemaVersion) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public RealtimeLogEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(ingestionId, "ingestionId must not be null");
        Objects.requireNonNull(applicationId, "applicationId must not be null");
        Objects.requireNonNull(applicationName, "applicationName must not be null");
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(logTimestamp, "logTimestamp must not be null");
        Objects.requireNonNull(processedAt, "processedAt must not be null");

        if (applicationName.isBlank()) {
            throw new IllegalArgumentException("applicationName must not be blank");
        }

        if (level.isBlank()) {
            throw new IllegalArgumentException("level must not be blank");
        }

        if (schemaVersion <= 0) {
            throw new IllegalArgumentException("schemaVersion must be positive");
        }
    }
}
