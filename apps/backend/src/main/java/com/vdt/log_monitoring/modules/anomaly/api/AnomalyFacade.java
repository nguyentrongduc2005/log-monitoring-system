package com.vdt.log_monitoring.modules.anomaly.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AnomalyFacade {

	AnomalyReportDto findReportById(UUID id);

	List<AnomalyReportDto> findReports(List<UUID> visibleApplicationIds);

	AnomalyReportDto createReport(CreateAnomalyReportCommand command);

	void markAlerted(UUID reportId, UUID alertId);

	void markAiPending(UUID reportId, String reason);

	void updateAiResult(UUID reportId, AnomalyAiResult result);

	void updateAiFailure(UUID reportId, String error);

	record CreateAnomalyReportCommand(
		UUID applicationId,
		String sourceType,
		String ruleName,
		String severity,
		String title,
		String summary,
		String hypothesis,
		Double confidenceScore,
		String likelihoodLabel,
		String impactSummary,
		String investigationStepsJson,
		String recommendedActionsJson,
		String dimensionType,
		String dimensionValue,
		String metricGroup,
		Double observedValue,
		Double thresholdValue,
		Long observedCount,
		Long thresholdCount,
		Instant windowStart,
		Instant windowEnd,
		String evidencePayloadJson,
		boolean aiTriggerRequested,
		String aiTriggerReason
	) {}

	record AnomalyReportDto(
		UUID id,
		UUID applicationId,
		UUID alertId,
		String sourceType,
		String ruleName,
		String severity,
		String status,
		String title,
		String summary,
		String hypothesis,
		Double confidenceScore,
		String likelihoodLabel,
		String impactSummary,
		String investigationStepsJson,
		String recommendedActionsJson,
		String dimensionType,
		String dimensionValue,
		String metricGroup,
		Double observedValue,
		Double thresholdValue,
		Long observedCount,
		Long thresholdCount,
		Instant windowStart,
		Instant windowEnd,
		String evidencePayloadJson,
		boolean aiTriggerRequested,
		String aiTriggerReason,
		String aiStatus,
		String aiModel,
		String aiPromptVersion,
		Instant aiStartedAt,
		Instant aiCompletedAt,
		String aiSummary,
		Double aiConfidenceScore,
		String aiLikelihoodLabel,
		String aiRootCauseCandidatesJson,
		String aiRecommendedActionsJson,
		String aiInvestigationStepsJson,
		String aiResultJson,
		String aiRawResponseJson,
		String aiError,
		Instant createdAt,
		Instant updatedAt
	) {}

	record AnomalyAiResult(
		String model,
		String promptVersion,
		String summary,
		Double confidenceScore,
		String likelihoodLabel,
		String rootCauseCandidatesJson,
		String recommendedActionsJson,
		String investigationStepsJson,
		String resultJson,
		String rawResponseJson
	) {}
}
