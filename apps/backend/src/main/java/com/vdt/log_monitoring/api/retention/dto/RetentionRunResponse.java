package com.vdt.log_monitoring.api.retention.dto;

import java.time.Instant;
import java.util.UUID;

import com.vdt.log_monitoring.modules.retention.api.RetentionFacade;

public record RetentionRunResponse(
	UUID id,
	UUID policyId,
	String status,
	Instant startedAt,
	Instant finishedAt,
	long affectedRows,
	String message
) {
	public static RetentionRunResponse from(RetentionFacade.RetentionRunDto run) {
		if (run == null) {
			return null;
		}
		return new RetentionRunResponse(
			run.id(),
			run.policyId(),
			run.status(),
			run.startedAt(),
			run.finishedAt(),
			run.affectedRows(),
			run.message()
		);
	}
}
