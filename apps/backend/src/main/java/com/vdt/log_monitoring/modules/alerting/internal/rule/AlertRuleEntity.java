package com.vdt.log_monitoring.modules.alerting.internal.rule;

import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(
	name = "alert_rules",
	schema = "alerting",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_alert_rules_application_name",
			columnNames = { "application_id", "name" }
		)
	},
	indexes = {
		@Index(name = "idx_alert_rules_application_status", columnList = "application_id,status"),
		@Index(name = "idx_alert_rules_min_severity", columnList = "min_severity")
	}
)
public class AlertRuleEntity {

	@Id
	private UUID id;

	@Column(name = "application_id", nullable = false)
	private UUID applicationId;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(name = "min_severity", nullable = false, length = 32)
	private AlertSeverity minSeverity;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private AlertSeverity severity;

	@Column(name = "keyword_pattern", length = 255)
	private String keywordPattern;

	@Column(name = "threshold_count", nullable = false)
	private int thresholdCount;

	@Column(name = "threshold_window_seconds", nullable = false)
	private int thresholdWindowSeconds;

	@Column(name = "cooldown_seconds", nullable = false)
	private int cooldownSeconds;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private AlertRuleStatus status;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(
		name = "alert_rule_channels",
		schema = "alerting",
		joinColumns = @JoinColumn(name = "rule_id")
	)
	private Set<AlertDeliveryTarget> deliveryTargets;

	@Column(name = "created_by", nullable = false)
	private UUID createdBy;

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

	public static AlertRuleEntity create(
		UUID applicationId,
		String name,
		String description,
		AlertSeverity minSeverity,
		AlertSeverity severity,
		String keywordPattern,
		int thresholdCount,
		int thresholdWindowSeconds,
		int cooldownSeconds,
		Set<AlertDeliveryTarget> deliveryTargets,
		UUID createdBy
	) {
		Instant now = Instant.now();
		return new AlertRuleEntity(
			UUID.randomUUID(),
			Objects.requireNonNull(applicationId, "applicationId must not be null"),
			requireText(name, "name"),
			trimOptional(description),
			Objects.requireNonNull(minSeverity, "minSeverity must not be null"),
			Objects.requireNonNull(severity, "severity must not be null"),
			trimOptional(keywordPattern),
			requirePositive(thresholdCount, "thresholdCount"),
			requirePositive(thresholdWindowSeconds, "thresholdWindowSeconds"),
			requirePositive(cooldownSeconds, "cooldownSeconds"),
			AlertRuleStatus.ACTIVE,
			copyDeliveryTargets(deliveryTargets),
			Objects.requireNonNull(createdBy, "createdBy must not be null"),
			now,
			now
		);
	}

	public void updateRule(
		String name,
		String description,
		AlertSeverity minSeverity,
		AlertSeverity severity,
		String keywordPattern,
		int thresholdCount,
		int thresholdWindowSeconds,
		int cooldownSeconds,
		Set<AlertDeliveryTarget> deliveryTargets
	) {
		this.name = requireText(name, "name");
		this.description = trimOptional(description);
		this.minSeverity = Objects.requireNonNull(minSeverity, "minSeverity must not be null");
		this.severity = Objects.requireNonNull(severity, "severity must not be null");
		this.keywordPattern = trimOptional(keywordPattern);
		this.thresholdCount = requirePositive(thresholdCount, "thresholdCount");
		this.thresholdWindowSeconds = requirePositive(thresholdWindowSeconds, "thresholdWindowSeconds");
		this.cooldownSeconds = requirePositive(cooldownSeconds, "cooldownSeconds");
		this.deliveryTargets = copyDeliveryTargets(deliveryTargets);
	}

	public void changeStatus(AlertRuleStatus status) {
		this.status = Objects.requireNonNull(status, "status must not be null");
	}

	public Set<AlertChannel> getChannels() {
		return deliveryTargets.stream()
			.map(AlertDeliveryTarget::getChannel)
			.collect(Collectors.toUnmodifiableSet());
	}

	public Set<AlertDeliveryTarget> getDeliveryTargets() {
		return Collections.unmodifiableSet(deliveryTargets);
	}

	public static Set<AlertDeliveryTarget> channelOnlyTargets(Set<AlertChannel> channels) {
		if (channels == null) {
			return Set.of();
		}
		return channels.stream()
			.map(AlertDeliveryTarget::channelOnly)
			.collect(Collectors.toUnmodifiableSet());
	}

	private static Set<AlertDeliveryTarget> copyDeliveryTargets(Set<AlertDeliveryTarget> deliveryTargets) {
		return AlertDeliveryTarget.copyOf(deliveryTargets);
	}

	private static int requirePositive(int value, String fieldName) {
		if (value <= 0) {
			throw new IllegalArgumentException(fieldName + " must be positive");
		}
		return value;
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
