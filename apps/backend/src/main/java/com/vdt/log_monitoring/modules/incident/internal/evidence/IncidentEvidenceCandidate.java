package com.vdt.log_monitoring.modules.incident.internal.evidence;

import java.time.Instant;
import java.util.UUID;

import com.vdt.log_monitoring.modules.incident.internal.incident.EvidenceType;

public record IncidentEvidenceCandidate(
	EvidenceType type,
	String sourceId,
	UUID applicationId,
	String fingerprint,
	String traceId,
	String severity,
	String summary,
	String sampleMessage,
	Instant occurredAt,
	String metadataJson
) {}
