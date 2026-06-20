package com.vdt.log_monitoring.modules.realtime.api.events;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AlertNotificationMessage(
	UUID alertId,
	UUID ruleId,
	UUID applicationId,
	String applicationName,
	String applicationDisplayName,
	String severity,
	String message,
	String fingerprint,
	Instant logTimestamp,
	Instant triggeredAt
) {

	public AlertNotificationMessage {
		Objects.requireNonNull(alertId, "alertId must not be null");
		Objects.requireNonNull(ruleId, "ruleId must not be null");
		Objects.requireNonNull(applicationId, "applicationId must not be null");
		Objects.requireNonNull(applicationName, "applicationName must not be null");
		Objects.requireNonNull(severity, "severity must not be null");
		Objects.requireNonNull(message, "message must not be null");
		Objects.requireNonNull(fingerprint, "fingerprint must not be null");
		Objects.requireNonNull(logTimestamp, "logTimestamp must not be null");
		Objects.requireNonNull(triggeredAt, "triggeredAt must not be null");

		if (applicationName.isBlank()) {
			throw new IllegalArgumentException("applicationName must not be blank");
		}

		if (severity.isBlank()) {
			throw new IllegalArgumentException("severity must not be blank");
		}

		if (message.isBlank()) {
			throw new IllegalArgumentException("message must not be blank");
		}
	}

}
