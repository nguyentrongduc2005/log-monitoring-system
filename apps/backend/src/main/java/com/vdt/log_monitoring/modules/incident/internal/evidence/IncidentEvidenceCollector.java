package com.vdt.log_monitoring.modules.incident.internal.evidence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.vdt.log_monitoring.modules.alerting.api.AlertingFacade;
import com.vdt.log_monitoring.modules.incident.internal.incident.EvidenceType;
import com.vdt.log_monitoring.modules.incident.internal.evidence.ProcessedLogEvidenceReader.FingerprintSummary;
import com.vdt.log_monitoring.modules.incident.internal.incident.IncidentAnomalyReportRepository;
import com.vdt.log_monitoring.modules.incident.internal.incident.IncidentAnomalyReportEntity;

@Service
@RequiredArgsConstructor
public class IncidentEvidenceCollector {

	private final AlertingFacade alertingFacade;
	private final ProcessedLogEvidenceReader processedLogEvidenceReader;
	private final IncidentAnomalyReportRepository anomalyReportRepository;

	public List<IncidentEvidenceCandidate> collect(
		List<UUID> applicationIds,
		List<UUID> alertIds,
		Instant windowStart,
		Instant windowEnd
	) {
		List<IncidentEvidenceCandidate> evidence = new ArrayList<>();
		evidence.addAll(collectAlertEvidence(applicationIds, alertIds, windowStart, windowEnd));
		
		for (UUID appId : applicationIds) {
			evidence.addAll(processedLogEvidenceReader.findTopErrorFingerprints(appId, windowStart, windowEnd)
				.stream()
				.map(summary -> fromTopFingerprint(appId, summary))
				.toList());
		}
		
		return evidence;
	}

	public List<IncidentEvidenceCandidate> collectFromTriggerAlert(
		UUID triggerAlertId,
		UUID applicationId,
		String fingerprint,
		Instant windowStart,
		Instant windowEnd
	) {
		List<IncidentEvidenceCandidate> evidence = new ArrayList<>();
		AlertingFacade.AlertDto triggerAlert = alertingFacade.findAlertById(triggerAlertId);
		evidence.add(fromAlert(triggerAlert, "TRIGGER_ALERT"));
		
		// Check for Anomaly Report
		anomalyReportRepository.findByAlertId(triggerAlertId)
			.ifPresent(report -> {
				evidence.add(new IncidentEvidenceCandidate(
					EvidenceType.LOG, 
					"ANOMALY:" + report.getId(), 
					applicationId, 
					null, 
					"CRITICAL", 
					"Anomaly detected: AI report requested", 
					report.getEvidencePayload(), 
					windowStart, 
					"{\"kind\":\"ANOMALY_EVIDENCE\"}"
				));
			});

		// fingerprint and eventId are no longer available in AlertDto since it supports global counting
		evidence.addAll(processedLogEvidenceReader.findTopErrorFingerprints(applicationId, windowStart, windowEnd)
			.stream()
			.map(summary -> fromTopFingerprint(applicationId, summary))
			.toList());
		return evidence;
	}

	private List<IncidentEvidenceCandidate> collectAlertEvidence(
		List<UUID> applicationIds,
		List<UUID> alertIds,
		Instant windowStart,
		Instant windowEnd
	) {
		if (alertIds == null || alertIds.isEmpty()) {
			return List.of();
		}
		return alertIds.stream()
			.distinct()
			.map(alertingFacade::findAlertById)
			.filter(alert -> inWindow(alert.triggeredAt(), windowStart, windowEnd))
			.map(alert -> fromAlert(alert, "RELATED_ALERT"))
			.toList();
	}

	private boolean inWindow(Instant timestamp, Instant windowStart, Instant windowEnd) {
		if (timestamp == null) {
			return false;
		}
		return !timestamp.isBefore(windowStart) && !timestamp.isAfter(windowEnd);
	}

	private IncidentEvidenceCandidate fromAlert(AlertingFacade.AlertDto alert, String kind) {
		String sample = alert.logSamples() != null && !alert.logSamples().isEmpty()
			? alert.logSamples().getFirst().message()
			: "Alert evaluation";
		String summary = "Alert " + alert.severity() + " for "
			+ displayName(alert.applicationDisplayName(), alert.applicationName())
			+ ": " + displayName(alert.ruleName(), sample);
		String metadata = "{"
			+ "\"kind\":\"" + escape(kind) + "\","
			+ "\"status\":\"" + escape(alert.status()) + "\","
			+ "\"ruleId\":\"" + alert.ruleId() + "\","
			+ "\"alertName\":\"" + escape(alert.ruleName()) + "\","
			+ "\"occurrenceCount\":" + alert.occurrenceCount() + ","
			+ "\"firstSeenAt\":\"" + alert.firstSeenAt() + "\","
			+ "\"lastSeenAt\":\"" + alert.lastSeenAt() + "\""
			+ "}";
		return new IncidentEvidenceCandidate(
			EvidenceType.ALERT,
			alert.id().toString(),
			alert.applicationId(),
			null, // fingerprint
			alert.severity(),
			summary,
			sample,
			alert.triggeredAt(),
			metadata);
	}

	private IncidentEvidenceCandidate fromTriggerFingerprint(UUID applicationId, FingerprintSummary summary) {
		return fromFingerprintSummary(
			applicationId,
			summary,
			"TRIGGER_FINGERPRINT",
			"Trigger fingerprint " + summary.fingerprint() + " appeared "
				+ summary.occurrenceCount() + " times");
	}

	private IncidentEvidenceCandidate fromTopFingerprint(UUID applicationId, FingerprintSummary summary) {
		return fromFingerprintSummary(
			applicationId,
			summary,
			"TOP_ERROR_FINGERPRINT",
			"Top error fingerprint " + summary.fingerprint() + " appeared "
				+ summary.occurrenceCount() + " times");
	}

	private IncidentEvidenceCandidate fromFingerprintSummary(
		UUID applicationId,
		FingerprintSummary summary,
		String kind,
		String text
	) {
		String metadata = "{"
			+ "\"kind\":\"" + escape(kind) + "\","
			+ "\"occurrenceCount\":" + summary.occurrenceCount() + ","
			+ "\"firstSeenAt\":\"" + summary.firstSeenAt() + "\","
			+ "\"lastSeenAt\":\"" + summary.lastSeenAt() + "\""
			+ "}";
		return new IncidentEvidenceCandidate(
			EvidenceType.LOG,
			sourceId(kind, summary.fingerprint() + ":" + summary.lastSeenAt().toEpochMilli()),
			applicationId,
			summary.fingerprint(),
			summary.severity(),
			text,
			summary.sampleMessage(),
			summary.firstSeenAt(),
			metadata);
	}



	private String displayName(String displayName, String fallback) {
		return displayName == null || displayName.isBlank() ? fallback : displayName;
	}

	private String escape(String value) {
		return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	private String sourceId(String prefix, String value) {
		String id = prefix + ":" + (value == null ? "" : value);
		return id.length() <= 128 ? id : id.substring(0, 128);
	}
}
