package com.vdt.log_monitoring.modules.retention.internal;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.vdt.log_monitoring.modules.retention.api.RetentionException;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "retention_policies", schema = "retention")
public class RetentionPolicyEntity {

	@Id
	private UUID id;

	@Column(name = "log_level", nullable = false, length = 32, unique = true)
	private String logLevel;

	@Column(nullable = false, length = 80)
	private String label;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(name = "retention_days", nullable = false)
	private int retentionDays;

	@Column(name = "min_days", nullable = false)
	private int minDays;

	@Column(name = "max_days", nullable = false)
	private int maxDays;

	@Column(nullable = false)
	private boolean enabled;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		createdAt = createdAt == null ? now : createdAt;
		updatedAt = updatedAt == null ? now : updatedAt;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

	public static RetentionPolicyEntity existing(
		UUID id,
		String logLevel,
		String label,
		String description,
		int retentionDays,
		int minDays,
		int maxDays,
		boolean enabled,
		int sortOrder,
		Instant createdAt,
		Instant updatedAt
	) {
		return new RetentionPolicyEntity(
			Objects.requireNonNull(id, "id must not be null"),
			requireText(logLevel, "logLevel"),
			requireText(label, "label"),
			trimOptional(description),
			retentionDays,
			minDays,
			maxDays,
			enabled,
			sortOrder,
			createdAt,
			updatedAt
		);
	}

	public void updateSettings(int retentionDays, boolean enabled) {
		if (retentionDays < minDays || retentionDays > maxDays) {
			throw new RetentionException(
				RetentionException.ErrorCode.INVALID_POLICY,
				"Retention days for " + logLevel + " must be between " + minDays + " and " + maxDays
			);
		}
		this.retentionDays = retentionDays;
		this.enabled = enabled;
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return value.trim();
	}

	private static String trimOptional(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
