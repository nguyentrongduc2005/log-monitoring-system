package com.vdt.log_monitoring.modules.processing.internal.pipeline;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.processing.api.ProcessingException;
import com.vdt.log_monitoring.modules.processing.internal.model.LogMetadata;
import com.vdt.log_monitoring.modules.processing.internal.model.LogProcessingStatus;
import com.vdt.log_monitoring.modules.processing.internal.model.ProcessedLog;
import com.vdt.log_monitoring.modules.processing.internal.model.RawLogEnvelope;

@Component
public class LogNormalizer {

    private final Clock clock;

    public LogNormalizer() {
        this(Clock.systemUTC());
    }

    LogNormalizer(Clock clock) {
        this.clock = clock;
    }

    public ProcessedLog normalize(RawLogEnvelope envelope, LogParser.ParsedLog parsedLog) {
        try {
            Instant processedAt = Instant.now(clock);
            return new ProcessedLog(
                    envelope.eventId(),
                    envelope.ingestionId(),
                    envelope.applicationId(),
                    envelope.applicationName().strip(),
                    normalizeNullable(envelope.applicationDisplayName()),
                    parsedLog.level(),
                    parsedLog.message().strip(),
                    normalizeNullable(parsedLog.traceId()),
                    parsedLog.logTimestamp(),
                    envelope.receivedAt(),
                    processedAt,
                    null,
                    LogProcessingStatus.NORMALIZED,
                    LogMetadata.empty());
        } catch (RuntimeException ex) {
            throw new ProcessingException(
                    ProcessingException.ErrorCode.RAW_LOG_NORMALIZATION_FAILED,
                    "Failed to normalize raw log",
                    ex);
        }
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }
}
