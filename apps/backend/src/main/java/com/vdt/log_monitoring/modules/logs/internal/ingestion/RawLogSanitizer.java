package com.vdt.log_monitoring.modules.logs.internal.ingestion;

public interface RawLogSanitizer {
    String redact(String rawLog);
}
