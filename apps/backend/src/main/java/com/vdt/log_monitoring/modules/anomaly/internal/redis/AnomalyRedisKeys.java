package com.vdt.log_monitoring.modules.anomaly.internal.redis;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.anomaly.internal.rule.AnomalyLogRule;
import com.vdt.log_monitoring.modules.anomaly.internal.rule.AnomalyMetricRule;

@Component
public class AnomalyRedisKeys {

	private static final int MAX_PART_LENGTH = 120;

	public String logCounter(UUID applicationId, AnomalyLogRule rule, String dimensionType, String dimensionValue) {
		return "anomaly:%s:log:%s:%s:%s".formatted(
			applicationId,
			rule.name(),
			sanitizePart(dimensionType),
			sanitizePart(dimensionValue));
	}

	public String logRuleActiveIndex(UUID applicationId, AnomalyLogRule rule) {
		return "anomaly:%s:log:%s:active".formatted(applicationId, rule.name());
	}

	public String logDimensionMember(String dimensionType, String dimensionValue) {
		return "%s:%s".formatted(sanitizePart(dimensionType), sanitizePart(dimensionValue));
	}

	public String logDedup(UUID applicationId, AnomalyLogRule rule, String dimensionType, String dimensionValue) {
		return "anomaly:%s:dedup:log:%s:%s:%s".formatted(
			applicationId,
			rule.name(),
			sanitizePart(dimensionType),
			sanitizePart(dimensionValue));
	}

	public String logDedupRepeats(UUID applicationId, AnomalyLogRule rule, String dimensionType, String dimensionValue) {
		return "anomaly:%s:dedup-repeats:log:%s:%s:%s".formatted(
			applicationId,
			rule.name(),
			sanitizePart(dimensionType),
			sanitizePart(dimensionValue));
	}

	public String metricSnapshot(UUID applicationId, AnomalyMetricRule rule) {
		return "anomaly:%s:metric:%s".formatted(applicationId, rule.name());
	}

	public String activeMetricRules(UUID applicationId) {
		return "anomaly:%s:metric-rules:active".formatted(applicationId);
	}

	public String activeMetricApplications() {
		return "anomaly:metric-applications:active";
	}

	public String metricDedup(UUID applicationId, String metricGroup) {
		return "anomaly:%s:dedup:metric:%s".formatted(applicationId, sanitizePart(metricGroup));
	}

	public String sanitizePart(String value) {
		if (value == null || value.isBlank()) {
			return "unknown";
		}
		String sanitized = value.trim()
			.replaceAll("\\s+", "_")
			.replace(':', '_');
		return sanitized.length() > MAX_PART_LENGTH ? sanitized.substring(0, MAX_PART_LENGTH) : sanitized;
	}
}
