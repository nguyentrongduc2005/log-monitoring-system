package com.vdt.log_monitoring.modules.processing.internal.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ProcessedLog(
        UUID eventId,
        UUID ingestionId,
        UUID applicationId,
        String applicationName,
        String applicationDisplayName,
        LogLevel level,
        String message,
        String traceId,
        Instant logTimestamp,
        Instant receivedAt,
        Instant processedAt,
        LogFingerprint fingerprint,
        LogProcessingStatus status,
        LogMetadata metadata) {

    public ProcessedLog {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(ingestionId, "ingestionId must not be null");
        Objects.requireNonNull(applicationId, "applicationId must not be null");
        Objects.requireNonNull(applicationName, "applicationName must not be null");
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(logTimestamp, "logTimestamp must not be null");
        Objects.requireNonNull(receivedAt, "receivedAt must not be null");
        Objects.requireNonNull(processedAt, "processedAt must not be null");
        Objects.requireNonNull(status, "status must not be null");

        if (applicationName.isBlank()) {
            throw new IllegalArgumentException("applicationName must not be blank");
        }

        if (message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }

        metadata = metadata == null ? LogMetadata.empty() : metadata;
    }

    public boolean shouldPublishCriticalAlert() {
        return level.isCriticalAlertLevel();
    }

    public ProcessedLog withStatus(LogProcessingStatus status) {
        return new ProcessedLog(
                eventId,
                ingestionId,
                applicationId,
                applicationName,
                applicationDisplayName,
                level,
                message,
                traceId,
                logTimestamp,
                receivedAt,
                processedAt,
                fingerprint,
                status,
                metadata);
    }

    public ProcessedLog withFingerprint(LogFingerprint fingerprint) {
        return new ProcessedLog(
                eventId,
                ingestionId,
                applicationId,
                applicationName,
                applicationDisplayName,
                level,
                message,
                traceId,
                logTimestamp,
                receivedAt,
                processedAt,
                fingerprint,
                status,
                metadata);
    }

    public ProcessedLog withMetadata(LogMetadata metadata) {
        return new ProcessedLog(
                eventId,
                ingestionId,
                applicationId,
                applicationName,
                applicationDisplayName,
                level,
                message,
                traceId,
                logTimestamp,
                receivedAt,
                processedAt,
                fingerprint,
                status,
                metadata);
    }
}
