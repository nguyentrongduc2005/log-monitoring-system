package com.vdt.log_monitoring.api.alerting.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.vdt.log_monitoring.modules.alerting.api.AlertingFacade;

public record AlertResponse(
	UUID id,
	UUID ruleId,
	UUID applicationId,
	UUID eventId,
	UUID ingestionId,
	String applicationName,
	String applicationDisplayName,
	String severity,
	String message,
	String fingerprint,
	Instant logTimestamp,
	Instant triggeredAt,
	String status,
	List<String> dispatchedChannels,
	List<AlertDeliveryTargetResponse> deliveryTargets,
	UUID acknowledgedBy,
	Instant acknowledgedAt,
	UUID resolvedBy,
	Instant resolvedAt,
	Instant createdAt,
	Instant updatedAt
) {

	public static AlertResponse from(AlertingFacade.AlertDto alert) {
		return new AlertResponse(
			alert.id(),
			alert.ruleId(),
			alert.applicationId(),
			alert.eventId(),
			alert.ingestionId(),
			alert.applicationName(),
			alert.applicationDisplayName(),
			alert.severity(),
			alert.message(),
			alert.fingerprint(),
			alert.logTimestamp(),
			alert.triggeredAt(),
			alert.status(),
			alert.dispatchedChannels(),
			alert.deliveryTargets().stream()
				.map(AlertDeliveryTargetResponse::from)
				.toList(),
			alert.acknowledgedBy(),
			alert.acknowledgedAt(),
			alert.resolvedBy(),
			alert.resolvedAt(),
			alert.createdAt(),
			alert.updatedAt()
		);
	}
}
