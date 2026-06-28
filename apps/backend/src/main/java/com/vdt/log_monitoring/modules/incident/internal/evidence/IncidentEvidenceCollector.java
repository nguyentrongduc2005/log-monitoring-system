package com.vdt.log_monitoring.modules.incident.internal.evidence;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.vdt.log_monitoring.modules.alerting.api.AlertingFacade;
import com.vdt.log_monitoring.modules.anomaly.api.AnomalyFacade;
import com.vdt.log_monitoring.modules.incident.internal.incident.EvidenceType;
import com.vdt.log_monitoring.modules.incident.internal.evidence.ProcessedLogEvidenceReader.FingerprintSummary;

@Service
@RequiredArgsConstructor
public class IncidentEvidenceCollector {

	private final AlertingFacade alertingFacade;
	private final ProcessedLogEvidenceReader processedLogEvidenceReader;
	private final AnomalyFacade anomalyFacade;

	public List<IncidentEvidenceCandidate> collectFromTriggerAlert(
		UUID triggerAlertId,
		UUID applicationId,
		Instant windowStart,
		Instant windowEnd
	) {
		List<IncidentEvidenceCandidate> evidence = new ArrayList<>();
		AlertingFacade.AlertDto triggerAlert = alertingFacade.findAlertById(triggerAlertId);
		evidence.add(fromAlert(triggerAlert, "TRIGGER_ALERT"));

		if (isAnomalyAlert(triggerAlert) && triggerAlert.sourceId() != null) {
			AnomalyFacade.AnomalyReportDto report = anomalyFacade.findReportById(triggerAlert.sourceId());
			evidence.add(new IncidentEvidenceCandidate(
				evidenceType(report),
				"ANOMALY_REPORT:" + report.id(),
				applicationId,
				null,
				report.severity(),
				report.title(),
				report.evidencePayloadJson(),
				report.windowStart(),
				"{\"kind\":\"ANOMALY_REPORT\",\"sourceType\":\"" + escape(report.sourceType()) + "\"}"));
		}

		// fingerprint and eventId are no longer available in AlertDto since it supports global counting
		evidence.addAll(processedLogEvidenceReader.findTopErrorFingerprints(applicationId, windowStart, windowEnd)
			.stream()
			.map(summary -> fromTopFingerprint(applicationId, summary))
			.toList());
		return evidence;
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
			+ "\"triggerType\":\"" + escape(alert.triggerType()) + "\","
			+ "\"sourceType\":\"" + escape(alert.sourceType()) + "\","
			+ "\"sourceId\":\"" + alert.sourceId() + "\","
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

	private IncidentEvidenceCandidate fromTopFingerprint(UUID applicationId, FingerprintSummary summary) {
		return fromFingerprintSummary(
			applicationId,
			summary,
			"TOP_ERROR_FINGERPRINT",
			"Top error fingerprint " + summary.fingerprint() + " appeared "
				+ summary.occurrenceCount() + " times from " + formatTime(summary.firstSeenAt()) + " to " + formatTime(summary.lastSeenAt()));
	}

	private String formatTime(Instant instant) {
		if (instant == null) {
			return "unknown";
		}
		return DateTimeFormatter.ofPattern("HH:mm:ss")
			.withZone(ZoneId.systemDefault())
			.format(instant);
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

	private boolean isAnomalyAlert(AlertingFacade.AlertDto alert) {
		return "ANOMALY_LOG".equals(alert.triggerType()) || "ANOMALY_METRIC".equals(alert.triggerType());
	}

	private EvidenceType evidenceType(AnomalyFacade.AnomalyReportDto report) {
		return "ANOMALY_METRIC".equals(report.sourceType()) ? EvidenceType.HEALTH : EvidenceType.LOG;
	}
}
