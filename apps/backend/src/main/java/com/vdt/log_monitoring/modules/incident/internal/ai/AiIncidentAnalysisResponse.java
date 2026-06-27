package com.vdt.log_monitoring.modules.incident.internal.ai;

import java.util.List;

import com.vdt.log_monitoring.modules.incident.internal.incident.AiConfidence;
import com.vdt.log_monitoring.modules.incident.internal.incident.IncidentSeverity;

public record AiIncidentAnalysisResponse(
	String provider,
	String model,
	String promptVersion,
	String summary,
	String likelyCause,
	IncidentSeverity severity,
	String severityReason,
	AiConfidence confidence,
	List<String> suggestedActions,
	List<String> evidenceRefs,
	String rawResponseJson
) {}
