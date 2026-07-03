package com.vdt.log_monitoring.modules.processing.internal.pipeline;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.processing.api.ProcessingException;
import com.vdt.log_monitoring.modules.processing.internal.model.LogLevel;
import com.vdt.log_monitoring.modules.processing.internal.model.RawLogEnvelope;

@Component
public class LogParser {

    private static final Pattern STRUCTURED_LOG_PATTERN = Pattern.compile(
            "^(?<timestamp>\\d{4}-\\d{2}-\\d{2}T\\S+)?\\s*\\[?(?<level>TRACE|DEBUG|INFO|WARN|ERROR|CRITICAL)\\]?\\s*(?<message>.*)$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TRACE_ID_PATTERN = Pattern.compile(
            "(?:traceId|trace_id|trace-id)=([A-Za-z0-9\\-_.]+)",
            Pattern.CASE_INSENSITIVE);

    public ParsedLog parse(RawLogEnvelope envelope) {
        if (envelope.schemaVersion() != 1) {
            throw new ProcessingException(
                    ProcessingException.ErrorCode.UNSUPPORTED_RAW_EVENT_SCHEMA,
                    "Unsupported raw log schema version: " + envelope.schemaVersion());
        }

        String rawLog = envelope.rawLog().strip();
        var matcher = STRUCTURED_LOG_PATTERN.matcher(rawLog);
        if (!matcher.matches()) {
            throw new ProcessingException(
                    ProcessingException.ErrorCode.RAW_LOG_PARSE_FAILED,
                    "Raw log does not match a supported format");
        }

        LogLevel level = parseLevel(matcher.group("level"));
        String message = normalizeMessage(matcher.group("message"), rawLog);
        Instant logTimestamp = parseTimestamp(matcher.group("timestamp"), envelope.receivedAt());
        String traceId = extractTraceId(rawLog);

        return new ParsedLog(level, message, traceId, logTimestamp);
    }

    private LogLevel parseLevel(String value) {
        try {
            return LogLevel.from(value);
        } catch (IllegalArgumentException ex) {
            throw new ProcessingException(
                    ProcessingException.ErrorCode.RAW_LOG_PARSE_FAILED,
                    "Unsupported raw log level",
                    ex);
        }
    }

    private String normalizeMessage(String message, String rawLog) {
        String normalized = message == null ? rawLog : message.strip();
        if (normalized.isBlank()) {
            throw new ProcessingException(
                    ProcessingException.ErrorCode.RAW_LOG_PARSE_FAILED,
                    "Raw log message is blank");
        }
        return normalized;
    }

    private Instant parseTimestamp(String value, Instant fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ex) {
            throw new ProcessingException(
                    ProcessingException.ErrorCode.RAW_LOG_PARSE_FAILED,
                    "Raw log timestamp is invalid",
                    ex);
        }
    }

    private String extractTraceId(String rawLog) {
        var matcher = TRACE_ID_PATTERN.matcher(rawLog);
        return matcher.find() ? matcher.group(1) : null;
    }

    public record ParsedLog(
            LogLevel level,
            String message,
            String traceId,
            Instant logTimestamp) {
    }
}
