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
@Table(name = "incident_ai_analyses", schema = "incident")
public class IncidentAiAnalysisEntity {

	@Id
	private UUID id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "incident_id", nullable = false)
	private IncidentEntity incident;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private AiAnalysisStatus status;

	@Column(length = 64)
	private String provider;

	@Column(length = 120)
	private String model;

	@Column(name = "prompt_version", nullable = false, length = 32)
	private String promptVersion;

	@Column(columnDefinition = "TEXT")
	private String summary;

	@Column(name = "likely_cause", columnDefinition = "TEXT")
	private String likelyCause;

	@Enumerated(EnumType.STRING)
	@Column(length = 32)
	private IncidentSeverity severity;

	@Column(name = "severity_reason", columnDefinition = "TEXT")
	private String severityReason;

	@Enumerated(EnumType.STRING)
	@Column(length = 32)
	private AiConfidence confidence;

	@Column(name = "suggested_actions_json", columnDefinition = "TEXT")
	private String suggestedActionsJson;

	@Column(name = "evidence_refs_json", columnDefinition = "TEXT")
	private String evidenceRefsJson;

	@Column(name = "raw_response_json", columnDefinition = "TEXT")
	private String rawResponseJson;

	@Column(name = "error_message", columnDefinition = "TEXT")
	private String errorMessage;

	@Column(name = "requested_by", nullable = false)
	private UUID requestedBy;

	@Column(name = "started_at")
	private Instant startedAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	void onCreate() {
		createdAt = createdAt == null ? Instant.now() : createdAt;
	}

	public static IncidentAiAnalysisEntity pending(IncidentEntity incident, UUID requestedBy) {
		return new IncidentAiAnalysisEntity(
			UUID.randomUUID(),
			Objects.requireNonNull(incident, "incident must not be null"),
			AiAnalysisStatus.PENDING,
			null,
			null,
			"v1",
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			Objects.requireNonNull(requestedBy, "requestedBy must not be null"),
			null,
			null,
			Instant.now());
	}

	public void markRunning(String provider, String model, String promptVersion) {
		this.status = AiAnalysisStatus.RUNNING;
		this.provider = trimOptional(provider);
		this.model = trimOptional(model);
		this.promptVersion = requireText(promptVersion, "promptVersion");
		this.startedAt = Instant.now();
		this.completedAt = null;
		this.errorMessage = null;
	}

	public void markSucceeded(
		String summary,
		String likelyCause,
		IncidentSeverity severity,
		String severityReason,
		AiConfidence confidence,
		String suggestedActionsJson,
		String evidenceRefsJson,
		String rawResponseJson
	) {
		this.status = AiAnalysisStatus.SUCCEEDED;
		this.summary = trimOptional(summary);
		this.likelyCause = trimOptional(likelyCause);
		this.severity = severity;
		this.severityReason = trimOptional(severityReason);
		this.confidence = confidence;
		this.suggestedActionsJson = trimOptional(suggestedActionsJson);
		this.evidenceRefsJson = trimOptional(evidenceRefsJson);
		this.rawResponseJson = trimOptional(rawResponseJson);
		this.errorMessage = null;
		this.completedAt = Instant.now();
	}

	public void markFailed(String message) {
		this.status = AiAnalysisStatus.FAILED;
		this.errorMessage = trimOptional(message);
		this.completedAt = Instant.now();
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
