package com.vdt.log_monitoring.modules.anomaly.internal.report;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.vdt.log_monitoring.modules.anomaly.api.AnomalyFacade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "anomaly_reports", schema = "anomaly", indexes = {
	@Index(name = "idx_anomaly_reports_application_created_at", columnList = "application_id,created_at"),
	@Index(name = "idx_anomaly_reports_source_rule_window", columnList = "source_type,rule_name,window_start,window_end"),
	@Index(name = "idx_anomaly_reports_open_identity", columnList = "application_id,source_type,rule_name,fingerprint,status")
})
public class AnomalyReportEntity {

	@Id
	private UUID id;

	@Column(name = "application_id", nullable = false)
	private UUID applicationId;

	@Column(name = "alert_id")
	private UUID alertId;

	@Column(name = "source_type", nullable = false, length = 32)
	private String sourceType;

	@Column(name = "rule_name", nullable = false, length = 120)
	private String ruleName;

	@Column(nullable = false, length = 255)
	private String fingerprint;

	@Column(nullable = false, length = 32)
	private String severity;

	@Column(nullable = false, length = 32)
	private String status;

	@Column(nullable = false, length = 180)
	private String title;

	@Column(columnDefinition = "TEXT")
	private String summary;

	@Column(columnDefinition = "TEXT")
	private String hypothesis;

	@Column(name = "confidence_score")
	private Double confidenceScore;

	@Column(name = "window_start", nullable = false)
	private Instant windowStart;

	@Column(name = "window_end", nullable = false)
	private Instant windowEnd;

	@Column(name = "occurrence_count", nullable = false)
	private long occurrenceCount;

	@Column(name = "first_seen_at", nullable = false)
	private Instant firstSeenAt;

	@Column(name = "last_seen_at", nullable = false)
	private Instant lastSeenAt;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "evidence_payload", nullable = false, columnDefinition = "jsonb")
	private String evidencePayloadJson;

	@Column(name = "ai_trigger_requested", nullable = false)
	private boolean aiTriggerRequested;

	@Column(name = "ai_trigger_reason", length = 120)
	private String aiTriggerReason;

	@Column(name = "ai_status", nullable = false, length = 32)
	private String aiStatus;

	@Column(name = "ai_started_at")
	private Instant aiStartedAt;

	@Column(name = "ai_completed_at")
	private Instant aiCompletedAt;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "ai_result", columnDefinition = "jsonb")
	private String aiResultJson;

	@Column(name = "ai_error", columnDefinition = "TEXT")
	private String aiError;

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

	public static AnomalyReportEntity create(AnomalyFacade.CreateAnomalyReportCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		AnomalyReportEntity report = new AnomalyReportEntity();
		report.id = UUID.randomUUID();
		report.applicationId = Objects.requireNonNull(command.applicationId(), "applicationId must not be null");
		report.sourceType = requireText(command.sourceType(), "sourceType");
		report.ruleName = requireText(command.ruleName(), "ruleName");
		report.fingerprint = trimToLength(requireText(command.fingerprint(), "fingerprint"), 255);
		report.severity = requireText(command.severity(), "severity");
		report.status = "DETECTED";
		report.title = trimToLength(requireText(command.title(), "title"), 180);
		report.summary = trimOptional(command.summary());
		report.hypothesis = trimOptional(command.hypothesis());
		report.confidenceScore = command.confidenceScore();
		report.windowStart = Objects.requireNonNull(command.windowStart(), "windowStart must not be null");
		report.windowEnd = Objects.requireNonNull(command.windowEnd(), "windowEnd must not be null");
		report.occurrenceCount = 1L;
		report.firstSeenAt = report.windowStart;
		report.lastSeenAt = report.windowEnd;
		report.evidencePayloadJson = jsonOrEmptyObject(command.evidencePayloadJson());
		report.aiTriggerRequested = command.aiTriggerRequested();
		report.aiTriggerReason = trimOptional(command.aiTriggerReason());
		report.aiStatus = "NOT_REQUESTED";
		return report;
	}

	public void recordOccurrence(AnomalyFacade.CreateAnomalyReportCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		String previousSeverity = severity;
		String incomingSeverity = requireText(command.severity(), "severity");
		boolean escalatedToCritical = isCritical(incomingSeverity) && !isCritical(previousSeverity);
		occurrenceCount++;
		windowStart = min(windowStart, Objects.requireNonNull(command.windowStart(), "windowStart must not be null"));
		windowEnd = max(windowEnd, Objects.requireNonNull(command.windowEnd(), "windowEnd must not be null"));
		firstSeenAt = min(firstSeenAt, command.windowStart());
		lastSeenAt = max(lastSeenAt, command.windowEnd());
		severity = strongerSeverity(previousSeverity, incomingSeverity);
		title = trimToLength(requireText(command.title(), "title"), 180);
		summary = trimOptional(command.summary());
		hypothesis = trimOptional(command.hypothesis());
		confidenceScore = max(confidenceScore, command.confidenceScore());
		evidencePayloadJson = jsonOrEmptyObject(command.evidencePayloadJson());
		if (command.aiTriggerRequested() && canRequestAi(escalatedToCritical)) {
			aiTriggerRequested = true;
			aiTriggerReason = trimOptional(command.aiTriggerReason());
			if (escalatedToCritical && !"PENDING".equals(aiStatus)) {
				aiStatus = "NOT_REQUESTED";
				aiStartedAt = null;
				aiCompletedAt = null;
				aiError = null;
			}
		}
	}

	public void markAlerted(UUID alertId) {
		this.alertId = Objects.requireNonNull(alertId, "alertId must not be null");
		if ("DETECTED".equals(status)) {
			status = "ALERTED";
		}
	}

	public void markAiPending(String reason) {
		if ("RESOLVED".equals(status)) {
			return;
		}
		aiTriggerRequested = true;
		aiTriggerReason = trimOptional(reason);
		aiStatus = "PENDING";
		status = "AI_PENDING";
		aiStartedAt = Instant.now();
		aiCompletedAt = null;
		aiError = null;
	}

	public void updateAiResult(AnomalyFacade.AnomalyAiResult result) {
		Objects.requireNonNull(result, "result must not be null");
		aiStatus = "SUCCEEDED";
		if (!"RESOLVED".equals(status)) {
			status = "AI_SUCCEEDED";
		}
		aiResultJson = jsonOrEmptyObject(result.resultJson());
		aiError = null;
		aiCompletedAt = Instant.now();
	}

	public void updateAiFailure(String error) {
		aiStatus = "FAILED";
		if (!"RESOLVED".equals(status)) {
			status = "AI_FAILED";
		}
		aiError = trimOptional(error);
		aiCompletedAt = Instant.now();
	}

	public void resolve(UUID resolvedBy) {
		this.status = "RESOLVED";
		this.resolvedBy = Objects.requireNonNull(resolvedBy, "resolvedBy must not be null");
		this.resolvedAt = Instant.now();
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

	private static String trimToLength(String value, int maxLength) {
		return value.length() <= maxLength ? value : value.substring(0, maxLength - 3) + "...";
	}

	private static String jsonOrEmptyObject(String value) {
		String trimmed = trimOptional(value);
		return trimmed == null ? "{}" : trimmed;
	}

	private boolean canRequestAi(boolean escalatedToCritical) {
		return "NOT_REQUESTED".equals(aiStatus)
			|| (escalatedToCritical && !"PENDING".equals(aiStatus));
	}

	private static Instant min(Instant current, Instant candidate) {
		if (current == null) {
			return candidate;
		}
		if (candidate == null) {
			return current;
		}
		return candidate.isBefore(current) ? candidate : current;
	}

	private static Instant max(Instant current, Instant candidate) {
		if (current == null) {
			return candidate;
		}
		if (candidate == null) {
			return current;
		}
		return candidate.isAfter(current) ? candidate : current;
	}

	private static Double max(Double current, Double candidate) {
		if (current == null) {
			return candidate;
		}
		if (candidate == null) {
			return current;
		}
		return Math.max(current, candidate);
	}

	private static String strongerSeverity(String current, String candidate) {
		return severityRank(candidate) > severityRank(current) ? candidate : current;
	}

	private static int severityRank(String severity) {
		if ("CRITICAL".equalsIgnoreCase(severity)) {
			return 4;
		}
		if ("ERROR".equalsIgnoreCase(severity)) {
			return 3;
		}
		if ("WARN".equalsIgnoreCase(severity) || "WARNING".equalsIgnoreCase(severity)) {
			return 2;
		}
		return 1;
	}

	private static boolean isCritical(String severity) {
		return "CRITICAL".equalsIgnoreCase(severity);
	}
}
