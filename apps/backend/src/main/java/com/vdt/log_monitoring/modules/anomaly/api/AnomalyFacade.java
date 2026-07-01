package com.vdt.log_monitoring.modules.anomaly.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AnomalyFacade {

	AnomalyReportDto findReportById(UUID id);

	List<AnomalyReportDto> findReports(List<UUID> visibleApplicationIds);

	AnomalyReportDto createReport(CreateAnomalyReportCommand command);

	AnomalyReportDto createOrUpdateReport(CreateAnomalyReportCommand command);

	void markAlerted(UUID reportId, UUID alertId);

	void markAiPending(UUID reportId, String reason);

	void updateAiResult(UUID reportId, AnomalyAiResult result);

	void updateAiFailure(UUID reportId, String error);

	AnomalyReportDto resolveReport(UUID reportId, UUID resolvedBy);

	record CreateAnomalyReportCommand(
		UUID applicationId,
		String sourceType,
		String ruleName,
		String fingerprint,
		String severity,
		String title,
		String summary,
		String hypothesis,
		Double confidenceScore,
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
		String fingerprint,
		String severity,
		String status,
		String title,
		String summary,
		String hypothesis,
		Double confidenceScore,
		Instant windowStart,
		Instant windowEnd,
		long occurrenceCount,
		Instant firstSeenAt,
		Instant lastSeenAt,
		String evidencePayloadJson,
		boolean aiTriggerRequested,
		String aiTriggerReason,
		String aiStatus,
		Instant aiStartedAt,
		Instant aiCompletedAt,
		String aiResultJson,
		String aiError,
		UUID resolvedBy,
		Instant resolvedAt,
		Instant createdAt,
		Instant updatedAt
	) {}

	record AnomalyAiResult(
		String resultJson
	) {}
}
