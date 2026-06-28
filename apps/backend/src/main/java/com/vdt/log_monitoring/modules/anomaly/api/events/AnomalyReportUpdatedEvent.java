package com.vdt.log_monitoring.modules.anomaly.api.events;

import java.time.Instant;
import java.util.UUID;

public record AnomalyReportUpdatedEvent(
	UUID anomalyReportId,
	UUID applicationId,
	String sourceType,
	String updateType,
	String aiStatus,
	Instant updatedAt
) {}
