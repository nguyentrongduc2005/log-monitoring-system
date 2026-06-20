package com.vdt.log_monitoring.modules.processing.internal.model;

import java.util.Objects;

public record LogFingerprint(String value) {

    public LogFingerprint {
        Objects.requireNonNull(value, "value must not be null");

        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }

    public static LogFingerprint of(String value) {
        return new LogFingerprint(value);
    }
}
