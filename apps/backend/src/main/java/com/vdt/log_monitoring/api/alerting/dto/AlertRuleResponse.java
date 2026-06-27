package com.vdt.log_monitoring.api.alerting.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.vdt.log_monitoring.modules.alerting.api.AlertingFacade;

public record AlertRuleResponse(
	UUID id,
	UUID applicationId,
	String name,
	String description,
	String minSeverity,
	String severity,
	String keywordPattern,
	int thresholdCount,
	int thresholdWindowSeconds,
	int cooldownSeconds,
	String status,
	List<String> channels,
	List<AlertDeliveryTargetResponse> deliveryTargets,
	UUID createdBy,
	Instant createdAt,
	Instant updatedAt
) {

	public static AlertRuleResponse from(AlertingFacade.AlertRuleDto rule) {
		return new AlertRuleResponse(
			rule.id(),
			rule.applicationId(),
			rule.name(),
			rule.description(),
			rule.minSeverity(),
			rule.severity(),
			rule.keywordPattern(),
			rule.thresholdCount(),
			rule.thresholdWindowSeconds(),
			rule.cooldownSeconds(),
			rule.status(),
			rule.channels(),
			rule.deliveryTargets().stream()
				.map(AlertDeliveryTargetResponse::from)
				.toList(),
			rule.createdBy(),
			rule.createdAt(),
			rule.updatedAt()
		);
	}
}
