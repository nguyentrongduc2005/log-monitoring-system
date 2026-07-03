package com.vdt.log_monitoring.modules.identity.internal.metricsource;

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

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "metric_sources", schema = "identity")
public class MetricSourceEntity {

	@Id
	private UUID id;

	@Column(name = "application_id", nullable = false, unique = true)
	private UUID applicationId;

	@Column(name = "target_host", nullable = false)
	private String targetHost;

	@Column(name = "target_port", nullable = false)
	private Integer targetPort;

	@Column(name = "metrics_path", nullable = false)
	private String metricsPath;

	@Column(name = "scrape_interval", nullable = false, length = 32)
	private String scrapeInterval;

	@Column(nullable = false)
	private boolean enabled;

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

	public static MetricSourceEntity create(
		UUID applicationId,
		String targetHost,
		Integer targetPort,
		String metricsPath,
		String scrapeInterval,
		boolean enabled
	) {
		Instant now = Instant.now();
		return new MetricSourceEntity(
			UUID.randomUUID(),
			Objects.requireNonNull(applicationId, "applicationId must not be null"),
			requireText(targetHost, "targetHost"),
			Objects.requireNonNull(targetPort, "targetPort must not be null"),
			requireText(metricsPath, "metricsPath"),
			requireText(scrapeInterval, "scrapeInterval"),
			enabled,
			now,
			now
		);
	}

	public void update(
		String targetHost,
		Integer targetPort,
		String metricsPath,
		String scrapeInterval,
		boolean enabled
	) {
		this.targetHost = requireText(targetHost, "targetHost");
		this.targetPort = Objects.requireNonNull(targetPort, "targetPort must not be null");
		this.metricsPath = requireText(metricsPath, "metricsPath");
		this.scrapeInterval = requireText(scrapeInterval, "scrapeInterval");
		this.enabled = enabled;
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return value.trim();
	}
}
