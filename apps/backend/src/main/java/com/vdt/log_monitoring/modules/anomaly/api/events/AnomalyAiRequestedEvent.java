package com.vdt.log_monitoring.modules.anomaly.api.events;

import java.time.Instant;
import java.util.UUID;

public record AnomalyAiRequestedEvent(
	UUID anomalyReportId,
	UUID applicationId,
	UUID alertId,
	String sourceType,
	String triggerReason,
	Instant requestedAt
) {}
