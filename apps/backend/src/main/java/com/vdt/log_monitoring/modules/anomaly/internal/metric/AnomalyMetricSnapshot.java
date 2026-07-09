package com.vdt.log_monitoring.modules.anomaly.internal.metric;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.vdt.log_monitoring.modules.anomaly.internal.rule.AnomalyMetricRule;

public record AnomalyMetricSnapshot(
	AnomalyMetricRule rule,
	UUID applicationId,
	double current,
	double avg,
	double max,
	long samples,
	List<Double> recentValues,
	Instant lastSeen
) {

	public static AnomalyMetricSnapshot from(AnomalyMetricRule rule, UUID applicationId, double current, Instant lastSeen) {
		List<Double> recentValues = List.of(current);
		return new AnomalyMetricSnapshot(
			rule,
			applicationId,
			current,
			current,
			current,
			recentValues.size(),
			recentValues,
			lastSeen);
	}
}
