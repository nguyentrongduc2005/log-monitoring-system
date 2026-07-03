package com.vdt.log_monitoring.modules.incident.internal.ai;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.vdt.log_monitoring.modules.incident.internal.evidence.IncidentEvidenceCandidate;

public record AiIncidentAnalysisRequest(
	UUID incidentId,
	String title,
	String description,
	List<UUID> applicationIds,
	Instant windowStart,
	Instant windowEnd,
	List<IncidentEvidenceCandidate> evidence
) {}
