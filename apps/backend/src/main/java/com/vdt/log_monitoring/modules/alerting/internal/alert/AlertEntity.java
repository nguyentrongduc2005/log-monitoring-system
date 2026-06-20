package com.vdt.log_monitoring.modules.alerting.internal.alert;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertChannel;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertDeliveryTarget;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertSeverity;

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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "alerts", schema = "alerting", indexes = {
		@Index(name = "idx_alerts_application_status", columnList = "application_id,status"),
		@Index(name = "idx_alerts_rule_triggered_at", columnList = "rule_id,triggered_at"),
		@Index(name = "idx_alerts_fingerprint", columnList = "fingerprint")
})
public class AlertEntity {

	@Id
	private UUID id;

	@Column(name = "rule_id", nullable = false)
	private UUID ruleId;

	@Column(name = "application_id", nullable = false)
	private UUID applicationId;

	@Column(name = "event_id", nullable = false)
	private UUID eventId;

	@Column(name = "ingestion_id", nullable = false)
	private UUID ingestionId;

	@Column(name = "application_name", nullable = false, length = 100)
	private String applicationName;

	@Column(name = "application_display_name", length = 150)
	private String applicationDisplayName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private AlertSeverity severity;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String message;

	@Column(nullable = false, length = 128)
	private String fingerprint;

	@Column(name = "log_timestamp", nullable = false)
	private Instant logTimestamp;

	@Column(name = "triggered_at", nullable = false)
	private Instant triggeredAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private AlertStatus status;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "alert_delivery_channels", schema = "alerting", joinColumns = @JoinColumn(name = "alert_id"))
	private Set<AlertDeliveryTarget> deliveryTargets;

	@Column(name = "acknowledged_by")
	private UUID acknowledgedBy;

	@Column(name = "acknowledged_at")
	private Instant acknowledgedAt;

	@Column(name = "resolved_by")
	private UUID resolvedBy;

	@Column(name = "resolved_at")
	private Instant resolvedAt;

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

	public static AlertEntity create(
			UUID ruleId,
			UUID applicationId,
			UUID eventId,
			UUID ingestionId,
			String applicationName,
			String applicationDisplayName,
			AlertSeverity severity,
			String message,
			String fingerprint,
			Instant logTimestamp,
			Set<AlertDeliveryTarget> deliveryTargets) {
		Instant now = Instant.now();
		return new AlertEntity(
				UUID.randomUUID(),
				Objects.requireNonNull(ruleId, "ruleId must not be null"),
				Objects.requireNonNull(applicationId, "applicationId must not be null"),
				Objects.requireNonNull(eventId, "eventId must not be null"),
				Objects.requireNonNull(ingestionId, "ingestionId must not be null"),
				requireText(applicationName, "applicationName"),
				trimOptional(applicationDisplayName),
				Objects.requireNonNull(severity, "severity must not be null"),
				requireText(message, "message"),
				requireText(fingerprint, "fingerprint"),
				Objects.requireNonNull(logTimestamp, "logTimestamp must not be null"),
				now,
				AlertStatus.OPEN,
				copyDeliveryTargets(deliveryTargets),
				null,
				null,
				null,
				null,
				now,
				now);
	}

	public void acknowledge(UUID acknowledgedBy) {
		this.status = AlertStatus.ACKNOWLEDGED;
		this.acknowledgedBy = Objects.requireNonNull(acknowledgedBy, "acknowledgedBy must not be null");
		this.acknowledgedAt = Instant.now();
	}

	public void resolve(UUID resolvedBy) {
		this.status = AlertStatus.RESOLVED;
		this.resolvedBy = Objects.requireNonNull(resolvedBy, "resolvedBy must not be null");
		this.resolvedAt = Instant.now();
	}

	public Set<AlertChannel> getDeliveryChannels() {
		return deliveryTargets.stream()
				.map(AlertDeliveryTarget::getChannel)
				.collect(Collectors.toUnmodifiableSet());
	}

	public Set<AlertDeliveryTarget> getDeliveryTargets() {
		return Collections.unmodifiableSet(deliveryTargets);
	}

	private static Set<AlertDeliveryTarget> copyDeliveryTargets(Set<AlertDeliveryTarget> deliveryTargets) {
		if (deliveryTargets == null || deliveryTargets.isEmpty()) {
			throw new IllegalArgumentException("deliveryTargets must not be empty");
		}
		Set<AlertDeliveryTarget> copy = new HashSet<>(deliveryTargets);
		if (copy.contains(null)) {
			throw new IllegalArgumentException("deliveryTargets must not contain null");
		}
		EnumSet<AlertChannel> channels = copy.stream()
				.map(AlertDeliveryTarget::getChannel)
				.collect(Collectors.toCollection(() -> EnumSet.noneOf(AlertChannel.class)));
		if (channels.size() != copy.size()) {
			throw new IllegalArgumentException("deliveryTargets must not contain duplicate channels");
		}
		return copy;
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
