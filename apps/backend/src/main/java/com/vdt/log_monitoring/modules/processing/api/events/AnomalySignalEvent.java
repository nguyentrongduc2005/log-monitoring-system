package com.vdt.log_monitoring.modules.processing.api.events;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import lombok.Builder;

@Builder
public record AnomalySignalEvent(
	UUID applicationId,
	String applicationName,
	String applicationDisplayName,
	Instant timestamp,
	UUID logId,
	String level,
	String matchedRule,
	String serviceName,
	String message,
	String fingerprint,
	String traceId,
	Map<String, String> attributes
) {}
