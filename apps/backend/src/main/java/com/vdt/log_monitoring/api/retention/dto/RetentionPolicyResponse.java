package com.vdt.log_monitoring.api.retention.dto;

import java.time.Instant;
import java.util.UUID;

import com.vdt.log_monitoring.modules.retention.api.RetentionFacade;

public record RetentionPolicyResponse(
	UUID id,
	String logLevel,
	String label,
	String description,
	int retentionDays,
	int minDays,
	int maxDays,
	boolean enabled,
	Instant nextRunAt,
	RetentionRunResponse recentOperation
) {
	public static RetentionPolicyResponse from(RetentionFacade.RetentionPolicyDto policy) {
		return new RetentionPolicyResponse(
			policy.id(),
			policy.logLevel(),
			policy.label(),
			policy.description(),
			policy.retentionDays(),
			policy.minDays(),
			policy.maxDays(),
			policy.enabled(),
			policy.nextRunAt(),
			RetentionRunResponse.from(policy.recentOperation())
		);
	}
}
