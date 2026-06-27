package com.vdt.log_monitoring.shared.event;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;

@Builder
public record AnomalySignalEvent(
	UUID applicationId,
	Instant timestamp,
	UUID logId,
	String level,
	String matchedRule,
	String serviceName
) {}
