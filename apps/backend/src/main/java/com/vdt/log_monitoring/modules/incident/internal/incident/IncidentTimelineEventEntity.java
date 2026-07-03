package com.vdt.log_monitoring.modules.incident.internal.incident;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "incident_timeline_events", schema = "incident")
public class IncidentTimelineEventEntity {

	@Id
	private UUID id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "incident_id", nullable = false)
	private IncidentEntity incident;

	@Column(name = "event_type", nullable = false, length = 64)
	private String eventType;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String message;

	@Column(name = "actor_user_id")
	private UUID actorUserId;

	@Column(name = "metadata_json", columnDefinition = "TEXT")
	private String metadataJson;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	void onCreate() {
		createdAt = createdAt == null ? Instant.now() : createdAt;
	}

	public static IncidentTimelineEventEntity create(
		IncidentEntity incident,
		String eventType,
		String message,
		UUID actorUserId,
		String metadataJson
	) {
		return new IncidentTimelineEventEntity(
			UUID.randomUUID(),
			Objects.requireNonNull(incident, "incident must not be null"),
			requireText(eventType, "eventType"),
			requireText(message, "message"),
			actorUserId,
			trimOptional(metadataJson),
			Instant.now());
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
