package com.vdt.log_monitoring.modules.anomaly.internal.log;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdt.log_monitoring.modules.anomaly.api.AnomalyException;
import com.vdt.log_monitoring.modules.anomaly.api.AnomalyFacade;
import com.vdt.log_monitoring.modules.anomaly.api.events.AnomalyDetectedEvent;
import com.vdt.log_monitoring.modules.anomaly.internal.config.AnomalyDetectionProperties;
import com.vdt.log_monitoring.modules.anomaly.internal.publisher.AnomalyDetectedPublisher;
import com.vdt.log_monitoring.modules.anomaly.internal.redis.AnomalyRedisKeys;
import com.vdt.log_monitoring.modules.anomaly.internal.rule.AnomalyLogRule;
import com.vdt.log_monitoring.modules.processing.api.events.AnomalySignalEvent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnomalyLogRuleHandler {

	private final AnomalyLogClassifier classifier;
	private final StringRedisTemplate redisTemplate;
	private final AnomalyRedisKeys keys;
	private final AnomalyDetectionProperties properties;
	private final ObjectMapper objectMapper;
	private final AnomalyFacade anomalyFacade;
	private final AnomalyDetectedPublisher anomalyDetectedPublisher;

	public void handle(AnomalySignalEvent event) {
		classifier.classify(event).ifPresent(match -> {
			AnomalyLogRule rule = match.rule();
			String counterKey = keys.logCounter(
				event.applicationId(),
				rule,
				match.dimensionType(),
				match.dimensionValue());
			Long count = redisTemplate.opsForValue().increment(counterKey);
			if (Long.valueOf(1L).equals(count)) {
				redisTemplate.expire(counterKey, rule.ttl());
			}

			String indexKey = keys.logRuleActiveIndex(event.applicationId(), rule);
			redisTemplate.opsForSet().add(indexKey, keys.logDimensionMember(match.dimensionType(), match.dimensionValue()));
			redisTemplate.expire(indexKey, properties.indexTtl());

			long observedCount = count == null ? 0 : count;
			if (!rule.isBreached(observedCount)) {
				return;
			}

			String dedupKey = keys.logDedup(event.applicationId(), rule, match.dimensionType(), match.dimensionValue());
			if (Boolean.TRUE.equals(redisTemplate.hasKey(dedupKey))) {
				String repeatsKey = keys.logDedupRepeats(event.applicationId(), rule, match.dimensionType(), match.dimensionValue());
				redisTemplate.opsForValue().increment(repeatsKey);
				redisTemplate.expire(repeatsKey, rule.cooldown().multipliedBy(6));
				return;
			}

			String repeatsKey = keys.logDedupRepeats(event.applicationId(), rule, match.dimensionType(), match.dimensionValue());
			long repeatedDedupCount = parseLong(redisTemplate.opsForValue().get(repeatsKey));
			Instant windowEnd = event.timestamp() == null ? Instant.now() : event.timestamp();
			Instant windowStart = windowEnd.minus(rule.window());
			String severity = rule.severityFor(observedCount);
			boolean aiTriggerRequested = rule.shouldTriggerAi(observedCount, repeatedDedupCount);
			String aiTriggerReason = aiTriggerRequested
				? aiTriggerReason(severity, observedCount, rule.thresholdCount(), repeatedDedupCount)
				: null;
			AnomalyFacade.AnomalyReportDto report = anomalyFacade.createReport(new AnomalyFacade.CreateAnomalyReportCommand(
				event.applicationId(),
				"ANOMALY_LOG",
				rule.name(),
				severity,
				title(rule, match.dimensionType(), match.dimensionValue()),
				summary(rule, observedCount, match.dimensionType(), match.dimensionValue()),
				rule.hypothesis(match.dimensionType(), match.dimensionValue()),
				rule.confidenceScore(observedCount),
				rule.likelihoodLabel(observedCount),
				"Log anomaly may indicate user-impacting degradation or security activity.",
				toJson(List.of(
					"Inspect logs with the same fingerprint and trace IDs in the anomaly window.",
					"Check whether the pattern correlates with deployments, traffic changes, or user actions.")),
				toJson(rule.recommendedActions()),
				match.dimensionType(),
				match.dimensionValue(),
				null,
				null,
				null,
				observedCount,
				rule.thresholdCount(),
				windowStart,
				windowEnd,
				evidencePayload(event, rule, match, observedCount, windowStart, windowEnd),
				aiTriggerRequested,
				aiTriggerReason));

			anomalyDetectedPublisher.publish(new AnomalyDetectedEvent(
				report.id(),
				event.applicationId(),
				applicationName(event),
				event.applicationDisplayName(),
				"ANOMALY_LOG",
				rule.name(),
				severity,
				report.title(),
				report.summary(),
				windowStart,
				windowEnd,
				aiTriggerRequested,
				aiTriggerReason,
				Instant.now()));
			redisTemplate.opsForValue().set(dedupKey, "1", rule.cooldown());
		});
	}

	private String evidencePayload(
		AnomalySignalEvent event,
		AnomalyLogRule rule,
		AnomalyLogMatch match,
		long observedCount,
		Instant windowStart,
		Instant windowEnd
	) {
		Map<String, Object> evidence = new LinkedHashMap<>();
		evidence.put("source", "LOG");
		evidence.put("ruleName", rule.name());
		evidence.put("dimensionType", match.dimensionType());
		evidence.put("dimensionValue", match.dimensionValue());
		evidence.put("observedCount", observedCount);
		evidence.put("thresholdCount", rule.thresholdCount());
		evidence.put("confidenceScore", rule.confidenceScore(observedCount));
		evidence.put("hypothesis", rule.hypothesis(match.dimensionType(), match.dimensionValue()));
		evidence.put("recommendedActions", rule.recommendedActions());
		evidence.put("windowStart", windowStart.toString());
		evidence.put("windowEnd", windowEnd.toString());
		evidence.put("sampleMessages", List.of(event.message()));
		evidence.put("fingerprints", event.fingerprint() == null || event.fingerprint().isBlank()
			? List.of()
			: List.of(event.fingerprint()));
		evidence.put("traceIds", event.traceId() == null || event.traceId().isBlank()
			? List.of()
			: List.of(event.traceId()));
		evidence.put("relatedLogIds", event.logId() == null ? List.of() : List.of(event.logId().toString()));
		return toJson(evidence);
	}

	private String title(AnomalyLogRule rule, String dimensionType, String dimensionValue) {
		return "Log anomaly " + rule.name() + " for " + dimensionType + " " + dimensionValue;
	}

	private String summary(AnomalyLogRule rule, long observedCount, String dimensionType, String dimensionValue) {
		return observedCount + " matching log events exceeded threshold " + rule.thresholdCount()
			+ " for " + dimensionType + " " + dimensionValue + ".";
	}

	private String aiTriggerReason(String severity, long count, long threshold, long repeatedDedupCount) {
		if ("CRITICAL".equals(severity)) {
			return "SEVERITY_CRITICAL";
		}
		if (repeatedDedupCount >= 2) {
			return "REPEATED_DEDUP";
		}
		if (count >= threshold * 3) {
			return "THRESHOLD_MULTIPLIER";
		}
		return "RULE_REQUIRES_AI";
	}

	private String applicationName(AnomalySignalEvent event) {
		if (event.applicationName() != null && !event.applicationName().isBlank()) {
			return event.applicationName();
		}
		return event.serviceName() == null || event.serviceName().isBlank() ? "application" : event.serviceName();
	}

	private long parseLong(String value) {
		if (value == null || value.isBlank()) {
			return 0;
		}
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException exception) {
			return 0;
		}
	}

	private String toJson(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException exception) {
			throw new AnomalyException(
				AnomalyException.ErrorCode.ANOMALY_PROCESSING_FAILED,
				"Failed to serialize anomaly report payload",
				exception);
		}
	}
}
