package com.vdt.log_monitoring.modules.anomaly.internal.metric;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.vdt.log_monitoring.modules.anomaly.internal.config.AnomalyDetectionProperties;
import com.vdt.log_monitoring.modules.anomaly.internal.redis.AnomalyRedisKeys;
import com.vdt.log_monitoring.modules.anomaly.internal.rule.AnomalyMetricRule;

class AnomalyMetricRuleHandlerTest {

	private static final UUID APP_ID = UUID.fromString("00000000-0000-0000-0000-000000000103");
	private static final Instant LAST_SEEN = Instant.parse("2026-06-28T00:00:00Z");

	private final StringRedisTemplate redisTemplate = org.mockito.Mockito.mock();
	private final ValueOperations<String, String> valueOperations = org.mockito.Mockito.mock();
	private final SetOperations<String, String> setOperations = org.mockito.Mockito.mock();
	private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
	private final AnomalyRedisKeys keys = new AnomalyRedisKeys();
	private final AnomalyDetectionProperties properties = new AnomalyDetectionProperties(
		Duration.ofMinutes(15),
		new AnomalyDetectionProperties.Metric(Duration.ofSeconds(30), Duration.ofSeconds(60), null),
		null,
		null);
	private final AnomalyMetricRuleHandler handler = new AnomalyMetricRuleHandler(
		redisTemplate,
		objectMapper,
		keys,
		properties);

	@BeforeEach
	void setUp() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(redisTemplate.opsForSet()).thenReturn(setOperations);
	}

	@Test
	void writesMetricSnapshotJsonAndIndexesRule() throws Exception {
		AnomalyMetricSnapshot snapshot = AnomalyMetricSnapshot.from(
			AnomalyMetricRule.CPU_USAGE,
			APP_ID,
			91.2,
			LAST_SEEN);
		String snapshotKey = keys.metricSnapshot(APP_ID, AnomalyMetricRule.CPU_USAGE);
		String indexKey = keys.activeMetricRules(APP_ID);
		when(valueOperations.get(snapshotKey)).thenReturn(null);

		handler.save(snapshot);

		ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
		verify(valueOperations).set(
			org.mockito.Mockito.eq(snapshotKey),
			jsonCaptor.capture(),
			org.mockito.Mockito.eq(Duration.ofSeconds(180)));
		JsonNode json = objectMapper.readTree(jsonCaptor.getValue());
		assertThat(json.path("ruleId").asText()).isEqualTo("CPU_USAGE");
		assertThat(json.path("current").asDouble()).isEqualTo(91.2);
		assertThat(json.path("avg").asDouble()).isEqualTo(91.2);
		assertThat(json.path("max").asDouble()).isEqualTo(91.2);
		assertThat(json.path("samples").asLong()).isEqualTo(1);
		assertThat(json.has("durationAboveThresholdSeconds")).isFalse();
		assertThat(json.has("score")).isFalse();
		assertThat(json.path("lastSeen").asText()).isEqualTo("2026-06-28T00:00:00Z");
		verify(setOperations).add(indexKey, "CPU_USAGE");
		verify(redisTemplate).expire(indexKey, Duration.ofMinutes(15));
	}

	@Test
	void updatesAverageMaxSamplesAndDurationFromPreviousSnapshot() throws Exception {
		AnomalyMetricSnapshot snapshot = AnomalyMetricSnapshot.from(
			AnomalyMetricRule.CPU_USAGE,
			APP_ID,
			91.2,
			LAST_SEEN);
		String snapshotKey = keys.metricSnapshot(APP_ID, AnomalyMetricRule.CPU_USAGE);
		when(valueOperations.get(snapshotKey)).thenReturn("""
			{
			  "ruleId": "CPU_USAGE",
			  "current": 84.0,
			  "avg": 85.0,
			  "max": 94.5,
			  "samples": 4,
			  "durationAboveThresholdSeconds": 120,
			  "score": 30,
			  "lastSeen": "2026-06-28T00:00:00Z"
			}
			""");

		handler.save(snapshot);

		ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
		verify(valueOperations).set(
			org.mockito.Mockito.eq(snapshotKey),
			jsonCaptor.capture(),
			org.mockito.Mockito.eq(Duration.ofSeconds(180)));
		JsonNode json = objectMapper.readTree(jsonCaptor.getValue());
		assertThat(json.path("current").asDouble()).isEqualTo(91.2);
		assertThat(json.path("avg").asDouble()).isEqualTo(86.24);
		assertThat(json.path("max").asDouble()).isEqualTo(94.5);
		assertThat(json.path("samples").asLong()).isEqualTo(5);
		assertThat(json.has("durationAboveThresholdSeconds")).isFalse();
		assertThat(json.has("score")).isFalse();
	}

	@Test
	void skipsSnapshotWithoutApplicationId() {
		AnomalyMetricSnapshot snapshot = AnomalyMetricSnapshot.from(
			AnomalyMetricRule.CPU_USAGE,
			null,
			91.2,
			LAST_SEEN);

		handler.save(snapshot);

		verify(redisTemplate, never()).opsForValue();
		verify(redisTemplate, never()).opsForSet();
	}
}
