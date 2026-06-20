package com.vdt.log_monitoring.modules.processing.internal.model;

import java.util.Arrays;

public enum LogLevel {
    INFO,
    WARN,
    ERROR,
    CRITICAL;

    public static LogLevel from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("log level must not be blank");
        }

        String normalized = value.trim().toUpperCase();
        return Arrays.stream(values())
                .filter(level -> level.name().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported log level: " + value));
    }

    public boolean isCriticalAlertLevel() {
        return this == ERROR || this == CRITICAL;
    }
}
