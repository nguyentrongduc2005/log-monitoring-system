package com.vdt.log_monitoring.api.retention.dto;

import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RetentionPolicyUpdateRequest(
	@NotNull UUID id,
	@Min(1) @Max(730) int retentionDays,
	boolean enabled
) {
}
