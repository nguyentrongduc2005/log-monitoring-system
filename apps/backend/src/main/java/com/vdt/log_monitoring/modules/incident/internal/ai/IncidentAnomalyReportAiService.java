package com.vdt.log_monitoring.modules.incident.internal.ai;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdt.log_monitoring.modules.anomaly.api.AnomalyFacade;
import com.vdt.log_monitoring.modules.incident.internal.evidence.AnomalyReportEvidenceContext;
import com.vdt.log_monitoring.modules.incident.internal.evidence.IncidentEvidenceCandidate;
import com.vdt.log_monitoring.modules.incident.internal.evidence.ProcessedLogEvidenceReader;
import com.vdt.log_monitoring.modules.incident.internal.evidence.ProcessedLogEvidenceReader.FingerprintSummary;
import com.vdt.log_monitoring.modules.incident.internal.evidence.ProcessedLogEvidenceReader.LogSample;
import com.vdt.log_monitoring.modules.incident.internal.incident.AiConfidence;
import com.vdt.log_monitoring.modules.incident.internal.incident.EvidenceType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IncidentAnomalyReportAiService {

	private final AnomalyFacade anomalyFacade;
	private final AiIncidentAnalysisClient aiIncidentAnalysisClient;
	private final ObjectMapper objectMapper;
	private final ProcessedLogEvidenceReader processedLogEvidenceReader;

	public void requestAnalysis(UUID reportId, UUID alertId, String triggerReason) {
		AnomalyFacade.AnomalyReportDto report = anomalyFacade.findReportById(reportId);
		List<IncidentEvidenceCandidate> evidence = evidence(report, alertId, triggerReason);
		try {
			AiIncidentAnalysisResponse response = aiIncidentAnalysisClient.analyze(new AiIncidentAnalysisRequest(
				report.id(),
				report.title(),
				description(report, alertId, triggerReason),
				List.of(report.applicationId()),
				report.windowStart(),
				report.windowEnd(),
				evidence));
			anomalyFacade.updateAiResult(report.id(), toAnomalyResult(response));
		} catch (RuntimeException exception) {
			anomalyFacade.updateAiFailure(report.id(), exception.getMessage());
		}
	}

	private List<IncidentEvidenceCandidate> evidence(
		AnomalyFacade.AnomalyReportDto report,
		UUID alertId,
		String triggerReason
	) {
		AnomalyReportEvidenceContext context = AnomalyReportEvidenceContext.from(report, objectMapper);
		List<IncidentEvidenceCandidate> evidence = new java.util.ArrayList<>();
		evidence.add(new IncidentEvidenceCandidate(
			"ANOMALY_METRIC".equals(report.sourceType()) ? EvidenceType.HEALTH : EvidenceType.LOG,
			"ANOMALY_REPORT:" + report.id(),
			report.applicationId(),
			context.fingerprints().isEmpty() ? null : context.fingerprints().getFirst(),
			report.severity(),
			anomalySummary(report, context),
			anomalySample(report, context),
			report.windowStart() == null ? Instant.now() : report.windowStart(),
			metadata(report, alertId, triggerReason, context)));

		Instant windowStart = report.windowStart() == null ? Instant.now().minusSeconds(300) : report.windowStart();
		Instant windowEnd = report.windowEnd() == null ? Instant.now() : report.windowEnd();
		processedLogEvidenceReader.findRelatedLogs(
				report.applicationId(),
				windowStart,
				windowEnd,
				context.traceIds(),
				context.fingerprints()).stream()
			.map(sample -> relatedLog(report.applicationId(), sample))
			.forEach(evidence::add);
		processedLogEvidenceReader.findTopSignalFingerprints(report.applicationId(), windowStart, windowEnd).stream()
			.map(summary -> fingerprintGroup(report.applicationId(), summary))
			.forEach(evidence::add);
		return evidence;
	}

	private String description(AnomalyFacade.AnomalyReportDto report, UUID alertId, String triggerReason) {
		return "Analyze anomaly report " + report.id()
			+ " from alert " + alertId
			+ ". Trigger reason: " + (triggerReason == null ? "unspecified" : triggerReason)
			+ ". Hypothesis: " + (report.hypothesis() == null ? "" : report.hypothesis());
	}

	private AnomalyFacade.AnomalyAiResult toAnomalyResult(AiIncidentAnalysisResponse response) {
		List<String> suggestedActions = response.suggestedActions() == null ? List.of() : response.suggestedActions();
		List<String> evidenceRefs = response.evidenceRefs() == null ? List.of() : response.evidenceRefs();
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("model", nullToBlank(response.model()));
		result.put("promptVersion", nullToBlank(response.promptVersion()));
		result.put("summary", nullToBlank(response.summary()));
		result.put("likelyCause", nullToBlank(response.likelyCause()));
		result.put("severity", response.severity() == null ? "" : response.severity().name());
		result.put("severityReason", nullToBlank(response.severityReason()));
		result.put("confidence", response.confidence() == null ? "" : response.confidence().name());
		result.put("confidenceScore", confidenceScore(response.confidence()));
		result.put("likelihoodLabel", likelihoodLabel(response.confidence()));
		result.put("rootCauseCandidates", List.of(Map.of(
				"cause", nullToBlank(response.likelyCause()),
				"reason", nullToBlank(response.severityReason()),
				"probability", confidenceScore(response.confidence()))));
		result.put("suggestedActions", suggestedActions);
		result.put("investigationSteps", suggestedActions);
		result.put("evidenceRefs", evidenceRefs);
		result.put("rawResponse", nullToBlank(response.rawResponseJson()));
		return new AnomalyFacade.AnomalyAiResult(toJson(result));
	}

	private IncidentEvidenceCandidate relatedLog(UUID applicationId, LogSample sample) {
		return new IncidentEvidenceCandidate(
			EvidenceType.LOG,
			"RELATED_LOG:" + nullToBlank(sample.eventId()),
			applicationId,
			sample.fingerprint(),
			sample.severity(),
			"Related log" + (sample.traceId() == null || sample.traceId().isBlank() ? "" : " traceId=" + sample.traceId()),
			sample.message(),
			sample.occurredAt(),
			toJson(Map.of(
				"kind", "RELATED_LOG",
				"traceId", nullToBlank(sample.traceId()),
				"fingerprint", nullToBlank(sample.fingerprint()))));
	}

	private IncidentEvidenceCandidate fingerprintGroup(UUID applicationId, FingerprintSummary summary) {
		return new IncidentEvidenceCandidate(
			EvidenceType.LOG,
			"TOP_SIGNAL_FINGERPRINT:" + nullToBlank(summary.fingerprint()),
			applicationId,
			summary.fingerprint(),
			summary.severity(),
			"Fingerprint " + summary.fingerprint() + " appeared " + summary.occurrenceCount() + " times",
			summary.sampleMessage(),
			summary.firstSeenAt(),
			toJson(Map.of(
				"kind", "TOP_SIGNAL_FINGERPRINT",
				"occurrenceCount", summary.occurrenceCount(),
				"firstSeenAt", String.valueOf(summary.firstSeenAt()),
				"lastSeenAt", String.valueOf(summary.lastSeenAt()))));
	}

	private String anomalySummary(AnomalyFacade.AnomalyReportDto report, AnomalyReportEvidenceContext context) {
		String countSummary = context.countSummary();
		String summary = report.summary() == null || report.summary().isBlank() ? report.title() : report.summary();
		return countSummary.isBlank() ? nullToBlank(summary) : nullToBlank(summary) + " (" + countSummary + ")";
	}

	private String anomalySample(AnomalyFacade.AnomalyReportDto report, AnomalyReportEvidenceContext context) {
		String sample = context.firstSampleMessage();
		if (!sample.isBlank()) {
			return sample;
		}
		return report.hypothesis() == null || report.hypothesis().isBlank() ? report.summary() : report.hypothesis();
	}

	private String metadata(
		AnomalyFacade.AnomalyReportDto report,
		UUID alertId,
		String triggerReason,
		AnomalyReportEvidenceContext context
	) {
		return toJson(Map.of(
			"kind", "ANOMALY_REPORT",
			"sourceType", nullToBlank(report.sourceType()),
			"ruleName", nullToBlank(report.ruleName()),
			"alertId", String.valueOf(alertId),
			"triggerReason", nullToBlank(triggerReason),
			"traceIds", context.traceIds(),
			"fingerprints", context.fingerprints(),
			"observedCount", context.observedCount() == null ? 0 : context.observedCount(),
			"thresholdCount", context.thresholdCount() == null ? 0 : context.thresholdCount(),
			"confidenceScore", report.confidenceScore() == null ? 0 : report.confidenceScore()));
	}

	private double confidenceScore(AiConfidence confidence) {
		if (confidence == AiConfidence.HIGH) {
			return 0.85;
		}
		if (confidence == AiConfidence.MEDIUM) {
			return 0.65;
		}
		return 0.40;
	}

	private String likelihoodLabel(AiConfidence confidence) {
		if (confidence == AiConfidence.HIGH) {
			return "HIGH";
		}
		if (confidence == AiConfidence.MEDIUM) {
			return "MEDIUM";
		}
		return "LOW";
	}

	private String nullToBlank(String value) {
		return value == null ? "" : value;
	}

	private String toJson(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Failed to serialize anomaly AI payload", exception);
		}
	}
}
