package com.vdt.log_monitoring.modules.ingestion.internal.impl;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.ingestion.internal.RawLogSanitizer;

@Component
public class RegexRawLogSanitizer implements RawLogSanitizer {
        private static final String REDACTED = "[REDACTED]";

        private static final Pattern SECRET_ASSIGNMENT = Pattern.compile(
                        "(?i)\\b(password|passwd|pwd|token|access[_-]?token|refresh[_-]?token|api[_-]?key|apikey|x[-_]?api[-_]?key|secret|client[_-]?secret)\\b\\s*([:=])\\s*(\"[^\"]*\"|'[^']*'|[^\\s,;]+)");

        private static final Pattern BEARER_TOKEN = Pattern.compile(
                        "(?i)(authorization\\s*[:=]\\s*bearer\\s+)([^\\s,;]+)");

        @Override
        public String redact(String rawLog) {
                if (rawLog == null || rawLog.isBlank()) {
                        return rawLog;
                }

                String redacted = SECRET_ASSIGNMENT
                                .matcher(rawLog)
                                .replaceAll("$1$2" + REDACTED);

                return BEARER_TOKEN
                                .matcher(redacted)
                                .replaceAll("$1" + REDACTED);
        }
}
