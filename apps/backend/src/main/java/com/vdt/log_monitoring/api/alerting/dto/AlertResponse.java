package com.vdt.log_monitoring.api.alerting.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.vdt.log_monitoring.modules.alerting.api.AlertingFacade;

public record AlertResponse(
	UUID id,
	UUID ruleId,
	String ruleName,
	UUID applicationId,
	String applicationName,
	String applicationDisplayName,
	String severity,
	String triggerType,
	String sourceType,
	UUID sourceId,
	String summary,
	String metadataJson,
	List<AlertLogSampleDto> logSamples,
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
			alert.ruleName(),
			alert.applicationId(),
			alert.applicationName(),
			alert.applicationDisplayName(),
			alert.severity(),
			alert.triggerType(),
			alert.sourceType(),
			alert.sourceId(),
			alert.summary(),
			alert.metadataJson(),
			alert.logSamples(),
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
