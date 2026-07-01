package com.vdt.log_monitoring.modules.incident.internal;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.vdt.log_monitoring.modules.alerting.api.AlertingFacade;
import com.vdt.log_monitoring.modules.anomaly.api.AnomalyFacade;
import com.vdt.log_monitoring.modules.incident.api.IncidentFacade;
import com.vdt.log_monitoring.modules.incident.internal.ai.IncidentAnomalyReportAiService;
import com.vdt.log_monitoring.modules.incident.internal.incident.IncidentAiAnalysisEntity;
import com.vdt.log_monitoring.modules.incident.internal.incident.IncidentAlertEntity;
import com.vdt.log_monitoring.modules.incident.internal.incident.IncidentApplicationEntity;
import com.vdt.log_monitoring.modules.incident.internal.incident.IncidentEntity;
import com.vdt.log_monitoring.modules.incident.internal.incident.IncidentService;
import com.vdt.log_monitoring.modules.incident.internal.incident.IncidentTimelineEventEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class IncidentFacadeImpl implements IncidentFacade {

	private final IncidentService incidentService;
	private final AlertingFacade alertingFacade;
	private final AnomalyFacade anomalyFacade;
	private final IncidentAnomalyReportAiService anomalyReportAiService;
	private final ObjectMapper objectMapper;

	@Override
	@Transactional
	public IncidentDto startFromAlert(StartFromAlertCommand command) {
		return mapIncident(incidentService.startFromAlert(command));
	}

	@Override
	@Transactional
	public IncidentDto resolveIncident(UUID incidentId, UUID resolvedBy) {
		return mapIncident(incidentService.resolveIncident(incidentId, resolvedBy));
	}

	@Override
	@Transactional(readOnly = true)
	public List<IncidentAnomalyReportDto> findAnomalyReports(List<UUID> visibleApplicationIds) {
		return anomalyFacade.findReports(visibleApplicationIds).stream()
				.map(this::mapAnomalyReport)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public IncidentAnomalyReportDto findAnomalyReportById(UUID id) {
		return mapAnomalyReport(anomalyFacade.findReportById(id));
	}

	@Override
	public void requestAnomalyReportAi(UUID reportId, UUID alertId, String triggerReason) {
		anomalyReportAiService.requestAnalysis(reportId, alertId, triggerReason);
	}

	private IncidentAnomalyReportDto mapAnomalyReport(AnomalyFacade.AnomalyReportDto report) {
		return new IncidentAnomalyReportDto(
				report.id(),
				report.alertId(),
				report.status(),
				report.evidencePayloadJson(),
				report.createdAt(),
				report.updatedAt());
	}

	@Override
	@Transactional(readOnly = true)
	public IncidentDto findIncidentById(UUID incidentId) {
		return mapIncident(incidentService.getIncidentById(incidentId));
	}

	@Override
	@Transactional(readOnly = true)
	public List<IncidentSummaryDto> findIncidents(List<UUID> visibleApplicationIds, String status, String severity) {
		return incidentService.listIncidents(visibleApplicationIds, status, severity).stream()
				.map(this::mapSummary)
				.toList();
	}

	private IncidentSummaryDto mapSummary(IncidentEntity incident) {
		return new IncidentSummaryDto(
				incident.getId(),
				incident.getTitle(),
				incident.getDescription(),
				summaryText(incident),
				impactText(incident, 0),
				incident.getStatus().name(),
				incident.getSeverity().name(),
				incident.getScope().name(),
				incident.getTriggerType().name(),
				incident.getStartedAt(),
				incident.getWindowStart(),
				incident.getWindowEnd(),
				incident.getLastEvidenceCollectedAt(),
				incident.applicationIds(),
				incident.getCreatedBy(),
				incident.getResolvedBy(),
				incident.getResolvedAt(),
				incident.getCreatedAt(),
				incident.getUpdatedAt());
	}

	private IncidentDto mapIncident(IncidentEntity incident) {
		IncidentAiAnalysisEntity latestAnalysis = latestAnalysis(incident);
		long relatedAlertOccurrences = relatedAlertOccurrences(incident);
		return new IncidentDto(
				incident.getId(),
				incident.getTitle(),
				incident.getDescription(),
				summaryText(incident),
				impactText(incident, relatedAlertOccurrences),
				latestAnalysis == null ? null : latestAnalysis.getLikelyCause(),
				latestAnalysis == null ? List.of() : suggestedActions(latestAnalysis.getSuggestedActionsJson()),
				incident.getStatus().name(),
				incident.getSeverity().name(),
				incident.getScope().name(),
				incident.getTriggerType().name(),
				incident.getStartedAt(),
				incident.getWindowStart(),
				incident.getWindowEnd(),
				incident.getLastEvidenceCollectedAt(),
				mapApplications(incident),
				mapEvidence(incident),
				mapTimeline(incident),
				incident.getCreatedBy(),
				incident.getResolvedBy(),
				incident.getResolvedAt(),
				incident.getCreatedAt(),
				incident.getUpdatedAt());
	}

	private List<ApplicationImpactDto> mapApplications(IncidentEntity incident) {
		return incident.getApplications().stream()
				.sorted(Comparator.comparing(application -> application.getApplicationId().toString()))
				.map(this::mapApplication)
				.toList();
	}

	private ApplicationImpactDto mapApplication(IncidentApplicationEntity application) {
		return new ApplicationImpactDto(
				application.getApplicationId(),
				application.getImpactRole().name(),
				application.getCreatedAt());
	}

	private List<EvidenceDto> mapEvidence(IncidentEntity incident) {
		return incident.getEvidence().stream()
				.map(this::mapEvidence)
				.toList();
	}

	private EvidenceDto mapEvidence(
			com.vdt.log_monitoring.modules.incident.internal.incident.IncidentEvidenceEntity evidence) {
		return new EvidenceDto(
				evidence.getId(),
				evidence.getType().name(),
				evidence.getSourceId(),
				evidence.getApplicationId(),
				evidence.getFingerprint(),
				evidence.getSeverity(),
				evidence.getSummary(),
				evidence.getSampleMessage(),
				evidence.getMetadataJson(),
				evidence.getOccurredAt());
	}

	private List<TimelineEventDto> mapTimeline(IncidentEntity incident) {
		return incident.getTimelineEvents().stream()
				.map(this::mapTimeline)
				.toList();
	}

	private TimelineEventDto mapTimeline(IncidentTimelineEventEntity event) {
		return new TimelineEventDto(
				event.getId(),
				event.getEventType(),
				event.getMessage(),
				event.getActorUserId(),
				event.getMetadataJson(),
				event.getCreatedAt());
	}

	private String summaryText(IncidentEntity incident) {
		IncidentAiAnalysisEntity latest = latestAnalysis(incident);
		if (latest != null && hasText(latest.getSummary())) {
			return latest.getSummary().trim();
		}
		if (hasText(incident.getDescription()) && !incident.getDescription().contains("fingerprint")) {
			return incident.getDescription().trim();
		}
		return "Incident is being investigated for the affected service.";
	}

	private String impactText(IncidentEntity incident, long occurrences) {
		String serviceText = incident.applicationIds().size() == 1
				? "1 affected service"
				: incident.applicationIds().size() + " affected services";
		if (occurrences > 0) {
			return serviceText + " with " + occurrences + " related alert occurrences.";
		}
		return serviceText + " currently marked " + incident.getSeverity().name() + ".";
	}

	private long relatedAlertOccurrences(IncidentEntity incident) {
		return incident.getAlerts().stream()
				.map(IncidentAlertEntity::getAlertId)
				.map(alertingFacade::findAlertById)
				.mapToLong(AlertingFacade.AlertDto::occurrenceCount)
				.sum();
	}

	private IncidentAiAnalysisEntity latestAnalysis(IncidentEntity incident) {
		return incident.getAnalyses().stream()
				.findFirst()
				.orElse(null);
	}

	private List<String> suggestedActions(String suggestedActionsJson) {
		if (!hasText(suggestedActionsJson)) {
			return List.of();
		}
		try {
			List<String> actions = objectMapper.readValue(
					suggestedActionsJson,
					new TypeReference<List<String>>() {
					});
			return actions.stream()
					.filter(this::hasText)
					.map(String::trim)
					.toList();
		} catch (JsonProcessingException exception) {
			return List.of();
		}
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
