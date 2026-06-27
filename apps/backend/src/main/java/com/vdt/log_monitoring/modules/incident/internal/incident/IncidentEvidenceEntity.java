package com.vdt.log_monitoring.modules.incident.internal.incident;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "incident_evidence", schema = "incident")
public class IncidentEvidenceEntity {

	@Id
	private UUID id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "incident_id", nullable = false)
	private IncidentEntity incident;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private EvidenceType type;

	@Column(name = "source_id", length = 128)
	private String sourceId;

	@Column(name = "application_id")
	private UUID applicationId;

	@Column(length = 128)
	private String fingerprint;

	@Column(name = "trace_id", length = 128)
	private String traceId;

	@Column(length = 32)
	private String severity;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String summary;

	@Column(name = "sample_message", columnDefinition = "TEXT")
	private String sampleMessage;

	@Column(name = "occurred_at")
	private Instant occurredAt;

	@Column(name = "metadata_json", columnDefinition = "TEXT")
	private String metadataJson;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	void onCreate() {
		createdAt = createdAt == null ? Instant.now() : createdAt;
	}

	public static IncidentEvidenceEntity create(
		IncidentEntity incident,
		EvidenceType type,
		String sourceId,
		UUID applicationId,
		String fingerprint,
		String traceId,
		String severity,
		String summary,
		String sampleMessage,
		Instant occurredAt,
		String metadataJson
	) {
		return new IncidentEvidenceEntity(
			UUID.randomUUID(),
			Objects.requireNonNull(incident, "incident must not be null"),
			Objects.requireNonNull(type, "type must not be null"),
			trimOptional(sourceId),
			applicationId,
			trimOptional(fingerprint),
			trimOptional(traceId),
			trimOptional(severity),
			requireText(summary, "summary"),
			trimOptional(sampleMessage),
			occurredAt,
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
