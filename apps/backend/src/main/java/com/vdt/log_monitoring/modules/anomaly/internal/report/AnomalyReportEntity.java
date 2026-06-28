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
	@Index(name = "idx_anomaly_reports_dimension", columnList = "application_id,rule_name,dimension_type,dimension_value")
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

	@Column(name = "likelihood_label", length = 32)
	private String likelihoodLabel;

	@Column(name = "impact_summary", columnDefinition = "TEXT")
	private String impactSummary;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "investigation_steps", columnDefinition = "jsonb")
	private String investigationStepsJson;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "recommended_actions", columnDefinition = "jsonb")
	private String recommendedActionsJson;

	@Column(name = "dimension_type", length = 64)
	private String dimensionType;

	@Column(name = "dimension_value")
	private String dimensionValue;

	@Column(name = "metric_group", length = 120)
	private String metricGroup;

	@Column(name = "observed_value")
	private Double observedValue;

	@Column(name = "threshold_value")
	private Double thresholdValue;

	@Column(name = "observed_count")
	private Long observedCount;

	@Column(name = "threshold_count")
	private Long thresholdCount;

	@Column(name = "window_start", nullable = false)
	private Instant windowStart;

	@Column(name = "window_end", nullable = false)
	private Instant windowEnd;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "evidence_payload", nullable = false, columnDefinition = "jsonb")
	private String evidencePayloadJson;

	@Column(name = "ai_trigger_requested", nullable = false)
	private boolean aiTriggerRequested;

	@Column(name = "ai_trigger_reason", length = 120)
	private String aiTriggerReason;

	@Column(name = "ai_status", nullable = false, length = 32)
	private String aiStatus;

	@Column(name = "ai_model", length = 120)
	private String aiModel;

	@Column(name = "ai_prompt_version", length = 64)
	private String aiPromptVersion;

	@Column(name = "ai_started_at")
	private Instant aiStartedAt;

	@Column(name = "ai_completed_at")
	private Instant aiCompletedAt;

	@Column(name = "ai_summary", columnDefinition = "TEXT")
	private String aiSummary;

	@Column(name = "ai_confidence_score")
	private Double aiConfidenceScore;

	@Column(name = "ai_likelihood_label", length = 32)
	private String aiLikelihoodLabel;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "ai_root_cause_candidates", columnDefinition = "jsonb")
	private String aiRootCauseCandidatesJson;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "ai_recommended_actions", columnDefinition = "jsonb")
	private String aiRecommendedActionsJson;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "ai_investigation_steps", columnDefinition = "jsonb")
	private String aiInvestigationStepsJson;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "ai_result", columnDefinition = "jsonb")
	private String aiResultJson;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "ai_raw_response", columnDefinition = "jsonb")
	private String aiRawResponseJson;

	@Column(name = "ai_error", columnDefinition = "TEXT")
	private String aiError;

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
		report.severity = requireText(command.severity(), "severity");
		report.status = "DETECTED";
		report.title = trimToLength(requireText(command.title(), "title"), 180);
		report.summary = trimOptional(command.summary());
		report.hypothesis = trimOptional(command.hypothesis());
		report.confidenceScore = command.confidenceScore();
		report.likelihoodLabel = trimOptional(command.likelihoodLabel());
		report.impactSummary = trimOptional(command.impactSummary());
		report.investigationStepsJson = jsonOrEmptyArray(command.investigationStepsJson());
		report.recommendedActionsJson = jsonOrEmptyArray(command.recommendedActionsJson());
		report.dimensionType = trimOptional(command.dimensionType());
		report.dimensionValue = trimOptional(command.dimensionValue());
		report.metricGroup = trimOptional(command.metricGroup());
		report.observedValue = command.observedValue();
		report.thresholdValue = command.thresholdValue();
		report.observedCount = command.observedCount();
		report.thresholdCount = command.thresholdCount();
		report.windowStart = Objects.requireNonNull(command.windowStart(), "windowStart must not be null");
		report.windowEnd = Objects.requireNonNull(command.windowEnd(), "windowEnd must not be null");
		report.evidencePayloadJson = jsonOrEmptyObject(command.evidencePayloadJson());
		report.aiTriggerRequested = command.aiTriggerRequested();
		report.aiTriggerReason = trimOptional(command.aiTriggerReason());
		report.aiStatus = command.aiTriggerRequested() ? "PENDING" : "NOT_REQUESTED";
		return report;
	}

	public void markAlerted(UUID alertId) {
		this.alertId = Objects.requireNonNull(alertId, "alertId must not be null");
		if ("DETECTED".equals(status)) {
			status = "ALERTED";
		}
	}

	public void markAiPending(String reason) {
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
		status = "AI_SUCCEEDED";
		aiModel = trimOptional(result.model());
		aiPromptVersion = trimOptional(result.promptVersion());
		aiSummary = trimOptional(result.summary());
		aiConfidenceScore = result.confidenceScore();
		aiLikelihoodLabel = trimOptional(result.likelihoodLabel());
		aiRootCauseCandidatesJson = jsonOrEmptyArray(result.rootCauseCandidatesJson());
		aiRecommendedActionsJson = jsonOrEmptyArray(result.recommendedActionsJson());
		aiInvestigationStepsJson = jsonOrEmptyArray(result.investigationStepsJson());
		aiResultJson = jsonOrEmptyObject(result.resultJson());
		aiRawResponseJson = trimOptional(result.rawResponseJson());
		aiError = null;
		aiCompletedAt = Instant.now();
	}

	public void updateAiFailure(String error) {
		aiStatus = "FAILED";
		status = "AI_FAILED";
		aiError = trimOptional(error);
		aiCompletedAt = Instant.now();
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

	private static String jsonOrEmptyArray(String value) {
		String trimmed = trimOptional(value);
		return trimmed == null ? "[]" : trimmed;
	}

	private static String jsonOrEmptyObject(String value) {
		String trimmed = trimOptional(value);
		return trimmed == null ? "{}" : trimmed;
	}
}
