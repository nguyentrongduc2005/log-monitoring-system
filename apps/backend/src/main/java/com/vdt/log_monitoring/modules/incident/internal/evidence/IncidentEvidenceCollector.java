package com.vdt.log_monitoring.modules.incident.internal.evidence;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdt.log_monitoring.modules.alerting.api.AlertingFacade;
import com.vdt.log_monitoring.modules.anomaly.api.AnomalyFacade;
import com.vdt.log_monitoring.modules.incident.internal.incident.EvidenceType;
import com.vdt.log_monitoring.modules.incident.internal.evidence.ProcessedLogEvidenceReader.FingerprintSummary;
import com.vdt.log_monitoring.modules.incident.internal.evidence.ProcessedLogEvidenceReader.LogSample;

@Service
@RequiredArgsConstructor
public class IncidentEvidenceCollector {

	private final AlertingFacade alertingFacade;
	private final ProcessedLogEvidenceReader processedLogEvidenceReader;
	private final AnomalyFacade anomalyFacade;
	private final ObjectMapper objectMapper;

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
			AnomalyReportEvidenceContext context = AnomalyReportEvidenceContext.from(report, objectMapper);
			evidence.add(fromAnomalyReport(report, context));
			evidence.addAll(processedLogEvidenceReader.findRelatedLogs(
					applicationId,
					windowStart,
					windowEnd,
					context.traceIds(),
					context.fingerprints()).stream()
				.map(sample -> fromRelatedLog(applicationId, sample))
				.toList());
		}

		evidence.addAll(processedLogEvidenceReader.findTopSignalFingerprints(applicationId, windowStart, windowEnd)
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
			"TOP_SIGNAL_FINGERPRINT",
			"Top signal fingerprint " + summary.fingerprint() + " appeared "
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

	private IncidentEvidenceCandidate fromAnomalyReport(
		AnomalyFacade.AnomalyReportDto report,
		AnomalyReportEvidenceContext context
	) {
		String countSummary = context.countSummary();
		String dimension = context.dimensionType().isBlank()
			? ""
			: " for " + context.dimensionType() + " " + context.dimensionValue();
		String sample = context.firstSampleMessage();
		String summary = displayName(report.title(), report.summary());
		if (!countSummary.isBlank()) {
			summary = summary + " (" + countSummary + dimension + ")";
		}
		return new IncidentEvidenceCandidate(
			evidenceType(report),
			"ANOMALY_REPORT:" + report.id(),
			report.applicationId(),
			context.fingerprints().isEmpty() ? null : context.fingerprints().getFirst(),
			report.severity(),
			summary,
			sample.isBlank() ? displayName(report.summary(), report.hypothesis()) : sample,
			report.windowStart(),
			toJson(java.util.Map.of(
				"kind", "ANOMALY_REPORT",
				"sourceType", displayName(report.sourceType(), ""),
				"ruleName", displayName(report.ruleName(), ""),
				"traceIds", context.traceIds(),
				"fingerprints", context.fingerprints(),
				"observedCount", context.observedCount() == null ? 0 : context.observedCount(),
				"thresholdCount", context.thresholdCount() == null ? 0 : context.thresholdCount())));
	}

	private IncidentEvidenceCandidate fromRelatedLog(UUID applicationId, LogSample sample) {
		String trace = sample.traceId() == null || sample.traceId().isBlank() ? "" : " traceId=" + sample.traceId();
		return new IncidentEvidenceCandidate(
			EvidenceType.LOG,
			sourceId("RELATED_LOG", sample.eventId()),
			applicationId,
			sample.fingerprint(),
			sample.severity(),
			"Related log" + trace,
			sample.message(),
			sample.occurredAt(),
			toJson(java.util.Map.of(
				"kind", "RELATED_LOG",
				"traceId", displayName(sample.traceId(), ""),
				"fingerprint", displayName(sample.fingerprint(), ""))));
	}

	private String displayName(String displayName, String fallback) {
		return displayName == null || displayName.isBlank() ? fallback : displayName;
	}

	private String escape(String value) {
		return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	private String toJson(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException exception) {
			return "{}";
		}
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
