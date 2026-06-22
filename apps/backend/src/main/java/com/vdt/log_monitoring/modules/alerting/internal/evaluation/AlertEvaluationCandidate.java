package com.vdt.log_monitoring.modules.alerting.internal.evaluation;

import java.time.Instant;
import java.util.UUID;

import com.vdt.log_monitoring.modules.alerting.internal.alert.AlertOccurrenceData;

public record AlertEvaluationCandidate(
	UUID eventId,
	UUID ingestionId,
	UUID applicationId,
	String applicationName,
	String applicationDisplayName,
	String severity,
	String message,
	String fingerprint,
	Instant logTimestamp
) {

	public AlertOccurrenceData toOccurrenceData() {
		return new AlertOccurrenceData(
			eventId,
			ingestionId,
			applicationId,
			applicationName,
			applicationDisplayName,
			message,
			fingerprint,
			logTimestamp);
	}
}
