package com.vdt.log_monitoring.modules.processing.internal.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.vdt.log_monitoring.modules.ingestion.api.events.RawLogReceivedEvent;

public record RawLogEnvelope(
        UUID eventId,
        UUID ingestionId,
        UUID applicationId,
        String applicationName,
        String applicationDisplayName,
        String rawLog,
        Instant receivedAt,
        int schemaVersion) {

    public RawLogEnvelope {
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

    public static RawLogEnvelope from(RawLogReceivedEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        return new RawLogEnvelope(
                event.eventId(),
                event.ingestionId(),
                event.applicationId(),
                event.applicationName(),
                event.applicationDisplayName(),
                event.rawLog(),
                event.receivedAt(),
                event.schemaVersion());
    }
}
