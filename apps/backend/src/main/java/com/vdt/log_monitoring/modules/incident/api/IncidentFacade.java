package com.vdt.log_monitoring.modules.incident.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface IncidentFacade {

	IncidentDto startFromAlert(StartFromAlertCommand command);

	IncidentDto resolveIncident(UUID incidentId, UUID resolvedBy);

	IncidentDto findIncidentById(UUID incidentId);

	void requestAnomalyReportAi(UUID reportId, UUID alertId, String triggerReason);

	List<IncidentAnomalyReportDto> findAnomalyReports(List<UUID> visibleApplicationIds);

	IncidentAnomalyReportDto findAnomalyReportById(UUID id);

	List<IncidentSummaryDto> findIncidents(List<UUID> visibleApplicationIds, String status, String severity);

	record StartFromAlertCommand(
		UUID alertId,
		UUID applicationId,
		String applicationName,
		String applicationDisplayName,
		String severity,
		List<com.vdt.log_monitoring.api.alerting.dto.AlertLogSampleDto> logSamples,
		Instant triggeredAt,
		UUID requestedBy
	) {}

	record IncidentAnomalyReportDto(
		UUID id,
		UUID alertId,
		String status,
		String evidencePayload,
		Instant createdAt,
		Instant updatedAt
	) {}

	record IncidentSummaryDto(
		UUID id,
		String title,
		String description,
		String shortSummary,
		String impact,
		String status,
		String severity,
		String scope,
		String triggerType,
		Instant startedAt,
		Instant windowStart,
		Instant windowEnd,
		Instant lastEvidenceCollectedAt,
		List<UUID> applicationIds,
		UUID createdBy,
		UUID resolvedBy,
		Instant resolvedAt,
		Instant createdAt,
		Instant updatedAt
	) {}

	record IncidentDto(
		UUID id,
		String title,
		String description,
		String shortSummary,
		String impact,
		String possibleCause,
		List<String> recommendedActions,
		String status,
		String severity,
		String scope,
		String triggerType,
		Instant startedAt,
		Instant windowStart,
		Instant windowEnd,
		Instant lastEvidenceCollectedAt,
		List<ApplicationImpactDto> applications,
		List<EvidenceDto> evidence,
		List<TimelineEventDto> timeline,
		UUID createdBy,
		UUID resolvedBy,
		Instant resolvedAt,
		Instant createdAt,
		Instant updatedAt
	) {}

	record ApplicationImpactDto(
		UUID applicationId,
		String impactRole,
		Instant createdAt
	) {}

	record EvidenceDto(
		UUID id,
		String type,
		String sourceId,
		UUID applicationId,
		String fingerprint,
		String severity,
		String summary,
		String sampleMessage,
		String metadataJson,
		Instant occurredAt
	) {}

	record TimelineEventDto(
		UUID id,
		String eventType,
		String message,
		UUID actorUserId,
		String metadataJson,
		Instant createdAt
	) {}
}
