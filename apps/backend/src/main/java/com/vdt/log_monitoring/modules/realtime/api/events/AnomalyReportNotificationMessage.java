package com.vdt.log_monitoring.modules.realtime.api.events;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AnomalyReportNotificationMessage(
	UUID anomalyReportId,
	UUID applicationId,
	String sourceType,
	String updateType,
	String aiStatus,
	Instant updatedAt
) {

	public AnomalyReportNotificationMessage {
		Objects.requireNonNull(anomalyReportId, "anomalyReportId must not be null");
		Objects.requireNonNull(applicationId, "applicationId must not be null");
		Objects.requireNonNull(updateType, "updateType must not be null");
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");
	}
}
