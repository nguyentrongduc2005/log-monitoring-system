package com.vdt.log_monitoring.modules.retention.internal;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "retention_runs", schema = "retention")
public class RetentionRunEntity {

	@Id
	private UUID id;

	@Column(name = "policy_id", nullable = false)
	private UUID policyId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private RetentionRunStatus status;

	@Column(name = "started_at", nullable = false)
	private Instant startedAt;

	@Column(name = "finished_at")
	private Instant finishedAt;

	@Column(name = "affected_rows", nullable = false)
	private long affectedRows;

	@Column(columnDefinition = "TEXT")
	private String message;

	public static RetentionRunEntity success(
		UUID policyId,
		Instant startedAt,
		Instant finishedAt,
		long affectedRows,
		String message
	) {
		return new RetentionRunEntity(
			UUID.randomUUID(),
			policyId,
			RetentionRunStatus.SUCCESS,
			startedAt,
			finishedAt,
			Math.max(affectedRows, 0),
			message
		);
	}

	public static RetentionRunEntity failed(
		UUID policyId,
		Instant startedAt,
		Instant finishedAt,
		String message
	) {
		return new RetentionRunEntity(
			UUID.randomUUID(),
			policyId,
			RetentionRunStatus.FAILED,
			startedAt,
			finishedAt,
			0,
			message
		);
	}
}
