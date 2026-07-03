package com.vdt.log_monitoring.modules.anomaly.api.events;

import java.time.Instant;
import java.util.UUID;

public record AnomalyDetectedEvent(
	UUID anomalyReportId,
	UUID applicationId,
	String applicationName,
	String applicationDisplayName,
	String sourceType,
	String ruleName,
	String severity,
	String title,
	String summary,
	Instant windowStart,
	Instant windowEnd,
	boolean aiTriggerRequested,
	String aiTriggerReason,
	Instant detectedAt
) {}
