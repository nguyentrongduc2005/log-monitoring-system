package com.vdt.log_monitoring.modules.alerting.internal.rule;

import java.util.Arrays;

public enum AlertSeverity {
	INFO,
	WARN,
	ERROR,
	CRITICAL;

	public static AlertSeverity from(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("alert severity must not be blank");
		}

		String normalized = value.trim().toUpperCase();
		return Arrays.stream(values())
			.filter(severity -> severity.name().equals(normalized))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Unsupported alert severity: " + value));
	}
}
