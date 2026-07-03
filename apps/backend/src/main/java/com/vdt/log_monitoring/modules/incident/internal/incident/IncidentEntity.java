package com.vdt.log_monitoring.modules.incident.internal.incident;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
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
@Table(name = "incidents", schema = "incident")
public class IncidentEntity {

	@Id
	private UUID id;

	@Column(nullable = false, length = 180)
	private String title;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private IncidentStatus status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private IncidentSeverity severity;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private IncidentScope scope;

	@Enumerated(EnumType.STRING)
	@Column(name = "trigger_type", nullable = false, length = 32)
	private IncidentTriggerType triggerType;

	@Column(name = "started_at", nullable = false)
	private Instant startedAt;

	@Column(name = "window_start", nullable = false)
	private Instant windowStart;

	@Column(name = "window_end", nullable = false)
	private Instant windowEnd;

	@Column(name = "last_evidence_collected_at")
	private Instant lastEvidenceCollectedAt;

	@Column(name = "created_by", nullable = false)
	private UUID createdBy;

	@Column(name = "resolved_by")
	private UUID resolvedBy;

	@Column(name = "resolved_at")
	private Instant resolvedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<IncidentApplicationEntity> applications;

	@OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<IncidentAlertEntity> alerts;

	@OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("createdAt ASC")
	private List<IncidentEvidenceEntity> evidence;

	@OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("createdAt DESC")
	private List<IncidentAiAnalysisEntity> analyses;

	@OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("createdAt ASC")
	private List<IncidentTimelineEventEntity> timelineEvents;

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

	public static IncidentEntity createFromAlert(
		String title,
		String description,
		UUID applicationId,
		IncidentSeverity severity,
		Instant windowStart,
		Instant windowEnd,
		UUID createdBy,
		UUID triggerAlertId
	) {
		if (applicationId == null) {
			throw new IllegalArgumentException("applicationId must not be null");
		}
		if (windowStart == null || windowEnd == null || windowStart.isAfter(windowEnd)) {
			throw new IllegalArgumentException("windowStart must be before or equal windowEnd");
		}
		IncidentEntity incident = new IncidentEntity(
			UUID.randomUUID(),
			requireText(title, "title"),
			trimOptional(description),
			IncidentStatus.INVESTIGATING,
			severity == null ? IncidentSeverity.UNKNOWN : severity,
			IncidentScope.APPLICATION,
			IncidentTriggerType.ALERT,
			Instant.now(),
			windowStart,
			windowEnd,
			null,
			Objects.requireNonNull(createdBy, "createdBy must not be null"),
			null,
			null,
			Instant.now(),
			Instant.now(),
			new ArrayList<>(),
			new ArrayList<>(),
			new ArrayList<>(),
			new ArrayList<>(),
			new ArrayList<>());
		incident.addApplication(applicationId, ImpactRole.PRIMARY);
		incident.addAlert(triggerAlertId, AlertRelationType.TRIGGER);
		incident.addTimelineEvent("CREATED", "Incident investigation started from alert", createdBy,
			"{\"triggerAlertId\":\"" + triggerAlertId + "\"}");
		return incident;
	}

	public void addApplication(UUID applicationId, ImpactRole impactRole) {
		boolean exists = applications.stream()
			.anyMatch(application -> application.getApplicationId().equals(applicationId));
		if (!exists) {
			applications.add(IncidentApplicationEntity.create(this, applicationId, impactRole));
		}
	}

	public void addAlert(UUID alertId, AlertRelationType relationType) {
		boolean exists = alerts.stream()
			.anyMatch(alert -> alert.getAlertId().equals(alertId));
		if (!exists) {
			alerts.add(IncidentAlertEntity.create(this, alertId, relationType));
		}
	}

	public void addEvidence(IncidentEvidenceEntity item) {
		evidence.add(Objects.requireNonNull(item, "item must not be null"));
	}

	public IncidentAiAnalysisEntity startAnalysis(UUID requestedBy) {
		IncidentAiAnalysisEntity analysis = IncidentAiAnalysisEntity.pending(this, requestedBy);
		analyses.add(analysis);
		addTimelineEvent("AI_STARTED", "AI investigation analysis started", requestedBy, null);
		return analysis;
	}

	public void markEvidenceCollected(Instant collectedUntil, UUID actorUserId) {
		Instant timestamp = Objects.requireNonNull(collectedUntil, "collectedUntil must not be null");
		if (timestamp.isAfter(windowEnd)) {
			windowEnd = timestamp;
		}
		lastEvidenceCollectedAt = timestamp;
		addTimelineEvent(
			"EVIDENCE_REFRESHED",
			"Incident evidence refreshed",
			actorUserId,
			"{\"collectedUntil\":\"" + timestamp + "\"}");
	}

	public void finishAnalysis(IncidentAiAnalysisEntity analysis) {
		if (analysis.getSeverity() != null) {
			severity = analysis.getSeverity();
		}
		addTimelineEvent("AI_COMPLETED", "AI investigation analysis completed", analysis.getRequestedBy(), null);
	}

	public void failAnalysis(IncidentAiAnalysisEntity analysis, String message) {
		addTimelineEvent("AI_FAILED", message, analysis.getRequestedBy(), null);
	}

	public void resolve(UUID resolvedBy) {
		if (status == IncidentStatus.RESOLVED) {
			return;
		}
		status = IncidentStatus.RESOLVED;
		this.resolvedBy = Objects.requireNonNull(resolvedBy, "resolvedBy must not be null");
		this.resolvedAt = Instant.now();
		addTimelineEvent("STATUS_CHANGED", "Incident resolved", resolvedBy, "{\"status\":\"RESOLVED\"}");
	}

	public void addTimelineEvent(String eventType, String message, UUID actorUserId, String metadataJson) {
		timelineEvents.add(IncidentTimelineEventEntity.create(this, eventType, message, actorUserId, metadataJson));
	}

	public List<UUID> applicationIds() {
		return applications.stream()
			.map(IncidentApplicationEntity::getApplicationId)
			.toList();
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
