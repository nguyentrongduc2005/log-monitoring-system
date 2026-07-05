package com.vdt.log_monitoring.modules.anomaly.internal.metric;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdt.log_monitoring.modules.anomaly.api.AnomalyException;
import com.vdt.log_monitoring.modules.anomaly.internal.config.AnomalyDetectionProperties;
import com.vdt.log_monitoring.modules.anomaly.internal.redis.AnomalyRedisKeys;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnomalyMetricRuleHandler {

	private static final int MAX_RECENT_VALUES = 6;

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;
	private final AnomalyRedisKeys keys;
	private final AnomalyDetectionProperties properties;

	public void save(AnomalyMetricSnapshot snapshot) {
		if (snapshot.applicationId() == null) {
			log.debug("Skipping anomaly metric snapshot without application id: rule={}", snapshot.rule());
			return;
		}

		String snapshotKey = keys.metricSnapshot(snapshot.applicationId(), snapshot.rule());
		AnomalyMetricSnapshot updatedSnapshot = updateState(snapshot, redisTemplate.opsForValue().get(snapshotKey));
		redisTemplate.opsForValue().set(
			snapshotKey,
			toJson(updatedSnapshot),
			properties.metric().effectiveFreshnessTtl());

		String indexKey = keys.activeMetricRules(snapshot.applicationId());
		redisTemplate.opsForSet().add(indexKey, snapshot.rule().name());
		redisTemplate.expire(indexKey, properties.indexTtl());

		String applicationIndexKey = keys.activeMetricApplications();
		redisTemplate.opsForSet().add(applicationIndexKey, snapshot.applicationId().toString());
		redisTemplate.expire(applicationIndexKey, properties.indexTtl());
	}

	private String toJson(AnomalyMetricSnapshot snapshot) {
		try {
			return objectMapper.writeValueAsString(Map.of(
				"ruleId", snapshot.rule().name(),
				"current", snapshot.current(),
				"avg", snapshot.avg(),
				"max", snapshot.max(),
				"samples", snapshot.samples(),
				"recentValues", snapshot.recentValues(),
				"lastSeen", snapshot.lastSeen().toString()));
		} catch (JsonProcessingException exception) {
			throw new AnomalyException(
				AnomalyException.ErrorCode.ANOMALY_PROCESSING_FAILED,
				"Failed to serialize anomaly metric snapshot",
				exception);
		}
	}

	private AnomalyMetricSnapshot updateState(AnomalyMetricSnapshot current, String previousJson) {
		PreviousSnapshot previous = parsePrevious(previousJson);
		List<Double> recentValues = appendRecentValue(previous.recentValues(), current.current());
		long samples = recentValues.size();
		double avg = recentValues.stream()
			.mapToDouble(Double::doubleValue)
			.average()
			.orElse(current.current());
		double max = recentValues.stream()
			.mapToDouble(Double::doubleValue)
			.max()
			.orElse(current.current());

		return new AnomalyMetricSnapshot(
			current.rule(),
			current.applicationId(),
			current.current(),
			avg,
			max,
			samples,
			recentValues,
			current.lastSeen());
	}

	private List<Double> appendRecentValue(List<Double> previousValues, double current) {
		List<Double> values = new ArrayList<>(previousValues);
		values.add(current);
		int fromIndex = Math.max(0, values.size() - MAX_RECENT_VALUES);
		return List.copyOf(values.subList(fromIndex, values.size()));
	}

	private PreviousSnapshot parsePrevious(String previousJson) {
		if (previousJson == null || previousJson.isBlank()) {
			return PreviousSnapshot.empty();
		}
		try {
			JsonNode json = objectMapper.readTree(previousJson);
			List<Double> recentValues = parseRecentValues(json);
			return new PreviousSnapshot(recentValues);
		} catch (Exception exception) {
			log.debug("Ignoring malformed anomaly metric snapshot state", exception);
			return PreviousSnapshot.empty();
		}
	}

	private List<Double> parseRecentValues(JsonNode json) {
		JsonNode valuesNode = json.path("recentValues");
		if (valuesNode.isArray() && !valuesNode.isEmpty()) {
			List<Double> values = new ArrayList<>();
			for (JsonNode valueNode : valuesNode) {
				if (valueNode.isNumber()) {
					values.add(valueNode.asDouble());
				}
			}
			if (!values.isEmpty()) {
				int fromIndex = Math.max(0, values.size() - MAX_RECENT_VALUES);
				return List.copyOf(values.subList(fromIndex, values.size()));
			}
		}
		if (json.path("current").isNumber()) {
			return List.of(json.path("current").asDouble());
		}
		return List.of();
	}

	private record PreviousSnapshot(
		List<Double> recentValues
	) {
		private static PreviousSnapshot empty() {
			return new PreviousSnapshot(List.of());
		}
	}
}
