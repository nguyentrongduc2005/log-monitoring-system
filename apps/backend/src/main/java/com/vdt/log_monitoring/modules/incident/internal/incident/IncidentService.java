package com.vdt.log_monitoring.modules.incident.internal.incident;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vdt.log_monitoring.modules.incident.api.IncidentException;
import com.vdt.log_monitoring.modules.incident.api.IncidentFacade;
import com.vdt.log_monitoring.modules.incident.internal.evidence.IncidentEvidenceCandidate;
import com.vdt.log_monitoring.modules.incident.internal.evidence.IncidentEvidenceCollector;
import com.vdt.log_monitoring.modules.incident.internal.investigation.IncidentAnalysisRunner;

@Service
@RequiredArgsConstructor
public class IncidentService {

	private static final Duration ALERT_CONTEXT_BEFORE = Duration.ofMinutes(30);
	private static final Duration ALERT_CONTEXT_AFTER = Duration.ofMinutes(5);

	private final IncidentRepository incidentRepository;
	private final IncidentEvidenceCollector evidenceCollector;
	private final IncidentAnalysisRunner analysisRunner;

	@Transactional
	public IncidentEntity startFromAlert(IncidentFacade.StartFromAlertCommand command) {
		validateAlertInvestigationRequest(command);
		List<IncidentEntity> linkedIncidents = incidentRepository.findOpenByAlertId(command.alertId());
		if (!linkedIncidents.isEmpty()) {
			return linkedIncidents.getFirst();
		}
		Instant anchor = command.triggeredAt();
		Instant windowStart = anchor.minus(ALERT_CONTEXT_BEFORE);
		Instant windowEnd = max(Instant.now(), anchor.plus(ALERT_CONTEXT_AFTER));
		IncidentEntity incident = IncidentEntity.createFromAlert(
			titleFromAlert(command),
			descriptionFromAlert(command),
			command.applicationId(),
			severityFromAlert(command.severity()),
			windowStart,
			windowEnd,
			command.requestedBy(),
			command.alertId());
		List<IncidentEvidenceCandidate> evidence = evidenceCollector.collectFromTriggerAlert(
			command.alertId(),
			command.applicationId(),
			windowStart,
			windowEnd);
		evidence.forEach(item -> incident.addEvidence(toEntity(incident, item)));
		incident.markEvidenceCollected(windowEnd, command.requestedBy());
		IncidentAiAnalysisEntity analysis = incident.startAnalysis(command.requestedBy());
		analysisRunner.run(incident, analysis, evidence);
		return incidentRepository.save(incident);
	}

	@Transactional
	public IncidentEntity resolveIncident(UUID incidentId, UUID resolvedBy) {
		IncidentEntity incident = getIncidentById(incidentId);
		incident.resolve(resolvedBy);
		return incident;
	}

	@Transactional(readOnly = true)
	public IncidentEntity getIncidentById(UUID incidentId) {
		return incidentRepository.findById(incidentId)
			.orElseThrow(() -> new IncidentException(
				IncidentException.ErrorCode.INCIDENT_NOT_FOUND,
				"Incident not found"));
	}

	@Transactional(readOnly = true)
	public List<IncidentEntity> listIncidents(List<UUID> visibleApplicationIds, String status, String severity) {
		if (visibleApplicationIds == null || visibleApplicationIds.isEmpty()) {
			return List.of();
		}
		return incidentRepository.findVisible(
			visibleApplicationIds,
			parseOptionalStatus(status),
			parseOptionalSeverity(severity));
	}

	private IncidentEvidenceEntity toEntity(IncidentEntity incident, IncidentEvidenceCandidate item) {
		return IncidentEvidenceEntity.create(
			incident,
			item.type(),
			item.sourceId(),
			item.applicationId(),
			item.fingerprint(),
			item.severity(),
			item.summary(),
			item.sampleMessage(),
			item.occurredAt(),
			item.metadataJson());
	}

	private void validateAlertInvestigationRequest(IncidentFacade.StartFromAlertCommand command) {
		if (command == null) {
			throw invalid("Alert investigation command is required");
		}
		if (command.alertId() == null) {
			throw invalid("alertId is required");
		}
		if (command.applicationId() == null) {
			throw invalid("applicationId is required");
		}
		if (command.requestedBy() == null) {
			throw invalid("requestedBy is required");
		}
		if (command.triggeredAt() == null) {
			throw invalid("Alert timestamp is required");
		}
	}

	private String titleFromAlert(IncidentFacade.StartFromAlertCommand command) {
		String appName = displayName(command.applicationDisplayName(), command.applicationName());
		String sample = command.logSamples() != null && !command.logSamples().isEmpty()
			? command.logSamples().getFirst().message()
			: null;
		String message = sample == null || sample.isBlank()
			? "Alert investigation"
			: sample.trim();
		String title = appName + ": " + message;
		return title.length() <= 180 ? title : title.substring(0, 177) + "...";
	}

	private String descriptionFromAlert(IncidentFacade.StartFromAlertCommand command) {
		return "Investigation started from alert " + command.alertId() + ".";
	}

	private String displayName(String displayName, String fallback) {
		if (displayName != null && !displayName.isBlank()) {
			return displayName.trim();
		}
		return fallback == null || fallback.isBlank() ? "Application" : fallback.trim();
	}

	private Instant max(Instant first, Instant second) {
		return first.isAfter(second) ? first : second;
	}

	private IncidentSeverity severityFromAlert(String severity) {
		if (severity == null || severity.isBlank()) {
			return IncidentSeverity.UNKNOWN;
		}
		return switch (severity.trim().toUpperCase(Locale.ROOT)) {
			case "CRITICAL" -> IncidentSeverity.SEV1;
			case "ERROR" -> IncidentSeverity.SEV2;
			case "WARN", "WARNING" -> IncidentSeverity.SEV3;
			default -> IncidentSeverity.UNKNOWN;
		};
	}

	private IncidentStatus parseOptionalStatus(String status) {
		if (status == null || status.isBlank()) {
			return null;
		}
		try {
			return IncidentStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new IncidentException(
				IncidentException.ErrorCode.INVALID_INCIDENT_STATUS,
				"Invalid incident status");
		}
	}

	private IncidentSeverity parseOptionalSeverity(String severity) {
		if (severity == null || severity.isBlank()) {
			return null;
		}
		try {
			return IncidentSeverity.valueOf(severity.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw invalid("Invalid incident severity");
		}
	}

	private IncidentException invalid(String message) {
		return new IncidentException(IncidentException.ErrorCode.INVALID_INCIDENT_REQUEST, message);
	}
}
