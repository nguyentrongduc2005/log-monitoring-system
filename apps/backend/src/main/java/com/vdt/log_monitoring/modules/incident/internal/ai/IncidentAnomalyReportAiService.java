package com.vdt.log_monitoring.modules.incident.internal.ai;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdt.log_monitoring.modules.anomaly.api.AnomalyFacade;
import com.vdt.log_monitoring.modules.incident.internal.evidence.IncidentEvidenceCandidate;
import com.vdt.log_monitoring.modules.incident.internal.incident.AiConfidence;
import com.vdt.log_monitoring.modules.incident.internal.incident.EvidenceType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IncidentAnomalyReportAiService {

	private final AnomalyFacade anomalyFacade;
	private final AiIncidentAnalysisClient aiIncidentAnalysisClient;
	private final ObjectMapper objectMapper;

	public void requestAnalysis(UUID reportId, UUID alertId, String triggerReason) {
		AnomalyFacade.AnomalyReportDto report = anomalyFacade.findReportById(reportId);
		try {
			AiIncidentAnalysisResponse response = aiIncidentAnalysisClient.analyze(new AiIncidentAnalysisRequest(
				report.id(),
				report.title(),
				description(report, alertId, triggerReason),
				List.of(report.applicationId()),
				report.windowStart(),
				report.windowEnd(),
				List.of(evidence(report, alertId, triggerReason))));
			anomalyFacade.updateAiResult(report.id(), toAnomalyResult(response));
		} catch (RuntimeException exception) {
			anomalyFacade.updateAiFailure(report.id(), exception.getMessage());
		}
	}

	private IncidentEvidenceCandidate evidence(
		AnomalyFacade.AnomalyReportDto report,
		UUID alertId,
		String triggerReason
	) {
		return new IncidentEvidenceCandidate(
			"ANOMALY_METRIC".equals(report.sourceType()) ? EvidenceType.HEALTH : EvidenceType.LOG,
			"ANOMALY_REPORT:" + report.id(),
			report.applicationId(),
			null,
			report.severity(),
			report.summary(),
			report.evidencePayloadJson(),
			report.windowStart() == null ? Instant.now() : report.windowStart(),
			metadata(report, alertId, triggerReason));
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
		String actionsJson = toJson(suggestedActions);
		String rootCauseJson = toJson(List.of(Map.of(
			"cause", nullToBlank(response.likelyCause()),
			"reason", nullToBlank(response.severityReason()),
			"probability", confidenceScore(response.confidence()))));
		String resultJson = toJson(Map.of(
			"summary", nullToBlank(response.summary()),
			"likelyCause", nullToBlank(response.likelyCause()),
			"severity", response.severity() == null ? "" : response.severity().name(),
			"severityReason", nullToBlank(response.severityReason()),
			"confidence", response.confidence() == null ? "" : response.confidence().name(),
			"suggestedActions", suggestedActions,
			"evidenceRefs", evidenceRefs));
		return new AnomalyFacade.AnomalyAiResult(
			response.model(),
			response.promptVersion(),
			response.summary(),
			confidenceScore(response.confidence()),
			likelihoodLabel(response.confidence()),
			rootCauseJson,
			actionsJson,
			actionsJson,
			resultJson,
			response.rawResponseJson());
	}

	private String metadata(AnomalyFacade.AnomalyReportDto report, UUID alertId, String triggerReason) {
		return toJson(Map.of(
			"kind", "ANOMALY_REPORT",
			"sourceType", nullToBlank(report.sourceType()),
			"alertId", String.valueOf(alertId),
			"triggerReason", nullToBlank(triggerReason),
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
