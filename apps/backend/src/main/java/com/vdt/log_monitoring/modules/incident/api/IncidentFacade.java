package com.vdt.log_monitoring.modules.incident.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface IncidentFacade {

	IncidentDto startFromAlert(StartFromAlertCommand command);

	IncidentDto resolveIncident(UUID incidentId, UUID resolvedBy);

	IncidentDto findIncidentById(UUID incidentId);

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
		List<ErrorLogDto> errorLogs,
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

	record ErrorLogDto(
		UUID eventId,
		UUID applicationId,
		String applicationName,
		String applicationDisplayName,
		String level,
		String message,
		String fingerprint,
		String traceId,
		Instant logTimestamp
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
