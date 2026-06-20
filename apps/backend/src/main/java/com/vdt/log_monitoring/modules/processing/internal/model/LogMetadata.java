package com.vdt.log_monitoring.modules.processing.internal.model;

import java.util.Map;

public record LogMetadata(
        String source,
        String host,
        String environment,
        Map<String, String> attributes) {

    public LogMetadata {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public static LogMetadata empty() {
        return new LogMetadata(null, null, null, Map.of());
    }
}
