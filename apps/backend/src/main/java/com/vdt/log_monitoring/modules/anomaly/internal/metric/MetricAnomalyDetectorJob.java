package com.vdt.log_monitoring.modules.anomaly.internal.metric;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdt.log_monitoring.modules.anomaly.api.AnomalyException;
import com.vdt.log_monitoring.modules.anomaly.api.AnomalyFacade;
import com.vdt.log_monitoring.modules.anomaly.api.events.AnomalyDetectedEvent;
import com.vdt.log_monitoring.modules.anomaly.internal.publisher.AnomalyDetectedPublisher;
import com.vdt.log_monitoring.modules.anomaly.internal.redis.AnomalyRedisKeys;
import com.vdt.log_monitoring.modules.anomaly.internal.rule.AnomalyMetricRule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MetricAnomalyDetectorJob {

	private static final String METRIC_GROUP = "RESOURCE_HEALTH";
	private static final String RULE_NAME = "RESOURCE_HEALTH_THRESHOLD";

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;
	private final AnomalyRedisKeys keys;
	private final AnomalyFacade anomalyFacade;
	private final AnomalyDetectedPublisher anomalyDetectedPublisher;

	@Scheduled(fixedRateString = "${app.anomaly.metric.detect-interval:30s}")
	public void detect() {
		Set<String> applicationIds = redisTemplate.opsForSet().members(keys.activeMetricApplications());
		if (applicationIds == null || applicationIds.isEmpty()) {
			return;
		}
		applicationIds.stream()
			.map(this::parseUuid)
			.flatMap(java.util.Optional::stream)
			.forEach(this::detectApplication);
	}

	private void detectApplication(UUID applicationId) {
		List<MetricState> states = readStates(applicationId);
		List<MetricState> breached = states.stream()
			.filter(state -> state.rule().isBreached(state.recentValues()))
			.toList();
		if (breached.isEmpty()) {
			return;
		}
		String dedupKey = keys.metricDedup(applicationId, METRIC_GROUP);
		if (Boolean.TRUE.equals(redisTemplate.hasKey(dedupKey))) {
			return;
		}

		Instant windowEnd = breached.stream()
			.map(MetricState::lastSeen)
			.max(Comparator.naturalOrder())
			.orElseGet(Instant::now);
		Instant windowStart = states.stream()
			.map(MetricState::lastSeen)
			.min(Comparator.naturalOrder())
			.orElse(windowEnd);
		String severity = aggregateSeverity(breached);
		boolean aiTriggerRequested = "CRITICAL".equals(severity) || breached.size() >= 2;
		String aiTriggerReason = aiTriggerRequested
			? ("CRITICAL".equals(severity) ? "SEVERITY_CRITICAL" : "MULTI_METRIC_BREACH")
			: null;
		double confidence = breached.stream()
			.mapToDouble(state -> state.rule().confidenceScore(state.current()))
			.max()
			.orElse(0.5);
		String title = "Resource anomaly detected for application " + applicationId;
		String summary = metricSummary(breached);
		AnomalyFacade.AnomalyReportDto report = anomalyFacade.createOrUpdateReport(new AnomalyFacade.CreateAnomalyReportCommand(
			applicationId,
			"ANOMALY_METRIC",
			RULE_NAME,
			METRIC_GROUP,
			severity,
			title,
				summary,
				"Resource metrics exceeded configured thresholds in the same evaluation window.",
				confidence,
				windowStart,
				windowEnd,
				evidencePayload(breached, states, windowStart, windowEnd, confidence),
			aiTriggerRequested,
			aiTriggerReason));
		boolean aiTriggerForEvent = shouldTriggerAiForEvent(report);

		anomalyDetectedPublisher.publish(new AnomalyDetectedEvent(
			report.id(),
			applicationId,
			applicationId.toString(),
			null,
			"ANOMALY_METRIC",
			RULE_NAME,
			severity,
			report.title(),
			report.summary(),
			windowStart,
			windowEnd,
			aiTriggerForEvent,
			aiTriggerForEvent ? aiTriggerReason : null,
			Instant.now()));
		redisTemplate.opsForValue().set(dedupKey, "1", java.time.Duration.ofMinutes(5));
	}

	private List<MetricState> readStates(UUID applicationId) {
		Set<String> ruleNames = redisTemplate.opsForSet().members(keys.activeMetricRules(applicationId));
		if (ruleNames == null || ruleNames.isEmpty()) {
			return List.of();
		}
		List<MetricState> states = new ArrayList<>();
		for (String ruleName : ruleNames) {
			try {
				AnomalyMetricRule rule = AnomalyMetricRule.valueOf(ruleName);
				String json = redisTemplate.opsForValue().get(keys.metricSnapshot(applicationId, rule));
				parseState(rule, json).ifPresent(states::add);
			} catch (IllegalArgumentException exception) {
				log.debug("Ignoring unknown metric anomaly rule {}", ruleName);
			}
		}
		return states;
	}

	private java.util.Optional<MetricState> parseState(AnomalyMetricRule rule, String json) {
		if (json == null || json.isBlank()) {
			return java.util.Optional.empty();
		}
		try {
			JsonNode root = objectMapper.readTree(json);
			return java.util.Optional.of(new MetricState(
				rule,
				root.path("current").asDouble(),
				root.path("avg").asDouble(),
				root.path("max").asDouble(),
				root.path("samples").asLong(),
				parseRecentValues(root),
				Instant.parse(root.path("lastSeen").asText())));
		} catch (Exception exception) {
			log.debug("Ignoring malformed metric anomaly snapshot", exception);
			return java.util.Optional.empty();
		}
	}

	private List<Double> parseRecentValues(JsonNode root) {
		JsonNode valuesNode = root.path("recentValues");
		if (valuesNode.isArray() && !valuesNode.isEmpty()) {
			List<Double> values = new ArrayList<>();
			for (JsonNode valueNode : valuesNode) {
				if (valueNode.isNumber()) {
					values.add(valueNode.asDouble());
				}
			}
			if (!values.isEmpty()) {
				return List.copyOf(values);
			}
		}
		if (root.path("current").isNumber()) {
			return List.of(root.path("current").asDouble());
		}
		return List.of();
	}

	private String evidencePayload(
		List<MetricState> breached,
		List<MetricState> states,
		Instant windowStart,
		Instant windowEnd,
		double confidence
	) {
		Map<String, Object> evidence = new LinkedHashMap<>();
		evidence.put("source", "METRIC");
		evidence.put("ruleName", RULE_NAME);
		evidence.put("windowStart", windowStart.toString());
		evidence.put("windowEnd", windowEnd.toString());
		evidence.put("confidenceScore", confidence);
		evidence.put("hypothesis", "Resource pressure detected in application metrics.");
		evidence.put("recommendedActions", List.of(
			"Check recent deployment or traffic spike for this application.",
			"Inspect CPU, memory, disk, and network saturation.",
			"Compare with application error logs in the same window."));
		evidence.put("breachedMetrics", breached.stream().map(this::metricEvidence).toList());
		evidence.put("normalMetrics", states.stream()
			.filter(state -> !state.rule().isBreached(state.recentValues()))
			.map(this::normalMetricEvidence)
			.toList());
		return toJson(evidence);
	}

	private Map<String, Object> metricEvidence(MetricState state) {
		Map<String, Object> metric = new LinkedHashMap<>();
		metric.put("metricName", state.rule().name());
		metric.put("currentValue", state.current());
		metric.put("thresholdValue", state.rule().warningThreshold());
		metric.put("criticalThresholdValue", state.rule().criticalThreshold());
		metric.put("unit", state.rule().unit());
		metric.put("avg", state.avg());
		metric.put("max", state.max());
		metric.put("samples", state.samples());
		metric.put("recentValues", state.recentValues());
		metric.put("warningBreachCount", breachCount(state.recentValues(), state.rule().warningThreshold()));
		metric.put("criticalBreachCount", breachCount(lastValues(state.recentValues(), 3), state.rule().criticalThreshold()));
		metric.put("lastSeenAt", state.lastSeen().toString());
		metric.put("severity", state.rule().severityFor(state.recentValues()));
		return metric;
	}

	private Map<String, Object> normalMetricEvidence(MetricState state) {
		Map<String, Object> metric = new LinkedHashMap<>();
		metric.put("metricName", state.rule().name());
		metric.put("currentValue", state.current());
		metric.put("unit", state.rule().unit());
		metric.put("samples", state.samples());
		metric.put("recentValues", state.recentValues());
		return metric;
	}

	private String aggregateSeverity(List<MetricState> breached) {
		if (breached.stream().anyMatch(state -> "CRITICAL".equals(state.rule().severityFor(state.recentValues())))) {
			return "CRITICAL";
		}
		if (breached.stream().anyMatch(state -> "ERROR".equals(state.rule().severityFor(state.recentValues())))) {
			return "ERROR";
		}
		return "WARN";
	}

	private String metricSummary(List<MetricState> breached) {
		return breached.stream()
			.map(state -> state.rule().name() + " " + format(state.current()) + state.rule().unit()
				+ " breached " + breachSummary(state))
			.reduce((left, right) -> left + ", " + right)
			.orElse("Metric threshold breached");
	}

	private String breachSummary(MetricState state) {
		if (state.rule().isCriticalBreached(state.recentValues())) {
			return breachCount(lastValues(state.recentValues(), 3), state.rule().criticalThreshold())
				+ "/3 recent samples >= " + format(state.rule().criticalThreshold()) + state.rule().unit();
		}
		return breachCount(state.recentValues(), state.rule().warningThreshold())
			+ "/6 recent samples >= " + format(state.rule().warningThreshold()) + state.rule().unit();
	}

	private long breachCount(List<Double> values, double threshold) {
		return values.stream()
			.filter(value -> value >= threshold)
			.count();
	}

	private List<Double> lastValues(List<Double> values, int count) {
		if (values.size() <= count) {
			return values;
		}
		return values.subList(values.size() - count, values.size());
	}

	private String format(double value) {
		return String.format(java.util.Locale.ROOT, "%.2f", value);
	}

	private boolean shouldTriggerAiForEvent(AnomalyFacade.AnomalyReportDto report) {
		return report.aiTriggerRequested()
			&& "NOT_REQUESTED".equals(report.aiStatus());
	}

	private java.util.Optional<UUID> parseUuid(String value) {
		try {
			return java.util.Optional.of(UUID.fromString(value));
		} catch (RuntimeException exception) {
			return java.util.Optional.empty();
		}
	}

	private String toJson(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException exception) {
			throw new AnomalyException(
				AnomalyException.ErrorCode.ANOMALY_PROCESSING_FAILED,
				"Failed to serialize metric anomaly payload",
				exception);
		}
	}

	private record MetricState(
		AnomalyMetricRule rule,
		double current,
		double avg,
		double max,
		long samples,
		List<Double> recentValues,
		Instant lastSeen
	) {}
}
