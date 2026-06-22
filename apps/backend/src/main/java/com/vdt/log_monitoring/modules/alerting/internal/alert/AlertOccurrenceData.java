package com.vdt.log_monitoring.modules.alerting.internal.alert;

import java.time.Instant;
import java.util.UUID;

public record AlertOccurrenceData(
	UUID eventId,
	UUID ingestionId,
	UUID applicationId,
	String applicationName,
	String applicationDisplayName,
	String message,
	String fingerprint,
	Instant logTimestamp
) {
}
