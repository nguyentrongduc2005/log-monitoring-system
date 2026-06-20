package com.vdt.log_monitoring.modules.ingestion.internal;

public interface RawLogSanitizer {
    String redact(String rawLog);
}
