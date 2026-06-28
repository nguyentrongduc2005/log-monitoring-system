package com.vdt.log_monitoring.modules.anomaly.internal.metric;

import java.time.Instant;
import java.util.UUID;

import com.vdt.log_monitoring.modules.anomaly.internal.rule.AnomalyMetricRule;

public record AnomalyMetricSnapshot(
	AnomalyMetricRule rule,
	UUID applicationId,
	double current,
	double avg,
	double max,
	long samples,
	Instant lastSeen
) {

	public static AnomalyMetricSnapshot from(AnomalyMetricRule rule, UUID applicationId, double current, Instant lastSeen) {
		return new AnomalyMetricSnapshot(
			rule,
			applicationId,
			current,
			current,
			current,
			1,
			lastSeen);
	}
}
