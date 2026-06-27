package com.vdt.log_monitoring.modules.incident.internal.investigation;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.vdt.log_monitoring.modules.incident.internal.ai.AiIncidentAnalysisClient;
import com.vdt.log_monitoring.modules.incident.internal.ai.AiIncidentAnalysisRequest;
import com.vdt.log_monitoring.modules.incident.internal.ai.AiIncidentAnalysisResponse;
import com.vdt.log_monitoring.modules.incident.internal.evidence.IncidentEvidenceCandidate;
import com.vdt.log_monitoring.modules.incident.internal.incident.IncidentAiAnalysisEntity;
import com.vdt.log_monitoring.modules.incident.internal.incident.IncidentEntity;

@Service
@RequiredArgsConstructor
public class IncidentAnalysisRunner {

	private final AiIncidentAnalysisClient aiIncidentAnalysisClient;

	public void run(IncidentEntity incident, IncidentAiAnalysisEntity analysis, List<IncidentEvidenceCandidate> evidence) {
		try {
			AiIncidentAnalysisRequest request = new AiIncidentAnalysisRequest(
				incident.getId(),
				incident.getTitle(),
				incident.getDescription(),
				incident.applicationIds(),
				incident.getWindowStart(),
				incident.getWindowEnd(),
				evidence);
			AiIncidentAnalysisResponse response = aiIncidentAnalysisClient.analyze(request);
			analysis.markRunning(response.provider(), response.model(), response.promptVersion());
			analysis.markSucceeded(
				response.summary(),
				response.likelyCause(),
				response.severity(),
				response.severityReason(),
				response.confidence(),
				toJsonArray(response.suggestedActions()),
				toJsonArray(response.evidenceRefs()),
				response.rawResponseJson());
			incident.finishAnalysis(analysis);
		} catch (Exception exception) {
			analysis.markFailed(exception.getMessage());
			incident.failAnalysis(analysis, "AI investigation analysis failed");
		}
	}

	private String toJsonArray(List<String> values) {
		if (values == null || values.isEmpty()) {
			return "[]";
		}
		return "[" + String.join(",", values.stream()
			.map(this::quote)
			.toList()) + "]";
	}

	private String quote(String value) {
		return "\"" + (value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"")) + "\"";
	}
}
