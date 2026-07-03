package com.vdt.log_monitoring.modules.anomaly.internal.metric;

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
		long samples = previous.samples() + 1;
		double avg = ((previous.avg() * previous.samples()) + current.current()) / samples;
		double max = Math.max(previous.max(), current.current());

		return new AnomalyMetricSnapshot(
			current.rule(),
			current.applicationId(),
			current.current(),
			avg,
			max,
			samples,
			current.lastSeen());
	}

	private PreviousSnapshot parsePrevious(String previousJson) {
		if (previousJson == null || previousJson.isBlank()) {
			return PreviousSnapshot.empty();
		}
		try {
			JsonNode json = objectMapper.readTree(previousJson);
			long samples = Math.max(0, json.path("samples").asLong(0));
			double avg = samples == 0 ? 0 : json.path("avg").asDouble(0);
			double max = samples == 0 ? Double.NEGATIVE_INFINITY : json.path("max").asDouble(Double.NEGATIVE_INFINITY);
			return new PreviousSnapshot(avg, max, samples);
		} catch (Exception exception) {
			log.debug("Ignoring malformed anomaly metric snapshot state", exception);
			return PreviousSnapshot.empty();
		}
	}

	private record PreviousSnapshot(
		double avg,
		double max,
		long samples
	) {
		private static PreviousSnapshot empty() {
			return new PreviousSnapshot(0, Double.NEGATIVE_INFINITY, 0);
		}
	}
}
