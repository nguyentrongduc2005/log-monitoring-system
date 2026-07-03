package com.vdt.log_monitoring.modules.retention.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface RetentionFacade {

	List<RetentionPolicyDto> findPolicies();

	List<RetentionPolicyDto> updatePolicies(List<UpdateRetentionPolicyCommand> commands);

	RetentionRunDto runPolicy(UUID policyId);

	void runDuePolicies();

	record UpdateRetentionPolicyCommand(
		UUID id,
		int retentionDays,
		boolean enabled
	) {
	}

	record RetentionPolicyDto(
		UUID id,
		String logLevel,
		String label,
		String description,
		int retentionDays,
		int minDays,
		int maxDays,
		boolean enabled,
		Instant nextRunAt,
		RetentionRunDto recentOperation
	) {
	}

	record RetentionRunDto(
		UUID id,
		UUID policyId,
		String status,
		Instant startedAt,
		Instant finishedAt,
		long affectedRows,
		String message
	) {
	}
}
