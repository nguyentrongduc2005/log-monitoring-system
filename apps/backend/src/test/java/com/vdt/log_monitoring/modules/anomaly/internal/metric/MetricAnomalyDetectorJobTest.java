package com.vdt.log_monitoring.modules.anomaly.internal.metric;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdt.log_monitoring.modules.anomaly.api.AnomalyFacade;
import com.vdt.log_monitoring.modules.anomaly.api.events.AnomalyDetectedEvent;
import com.vdt.log_monitoring.modules.anomaly.internal.publisher.AnomalyDetectedPublisher;
import com.vdt.log_monitoring.modules.anomaly.internal.redis.AnomalyRedisKeys;

class MetricAnomalyDetectorJobTest {

	private static final UUID APP_ID = UUID.fromString("00000000-0000-0000-0000-000000000103");
	private static final UUID REPORT_ID = UUID.fromString("00000000-0000-0000-0000-000000000203");
	private static final Instant LAST_SEEN = Instant.parse("2026-06-28T00:00:00Z");

	private final StringRedisTemplate redisTemplate = org.mockito.Mockito.mock();
	private final ValueOperations<String, String> valueOperations = org.mockito.Mockito.mock();
	private final SetOperations<String, String> setOperations = org.mockito.Mockito.mock();
	private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
	private final AnomalyRedisKeys keys = new AnomalyRedisKeys();
	private final AnomalyFacade anomalyFacade = org.mockito.Mockito.mock();
	private final AnomalyDetectedPublisher anomalyDetectedPublisher = org.mockito.Mockito.mock();
	private final MetricAnomalyDetectorJob job = new MetricAnomalyDetectorJob(
		redisTemplate,
		objectMapper,
		keys,
		anomalyFacade,
		anomalyDetectedPublisher);

	@BeforeEach
	void setUp() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(redisTemplate.opsForSet()).thenReturn(setOperations);
		when(setOperations.members(keys.activeMetricApplications())).thenReturn(Set.of(APP_ID.toString()));
		when(setOperations.members(keys.activeMetricRules(APP_ID))).thenReturn(Set.of("CPU_USAGE"));
		when(redisTemplate.hasKey(keys.metricDedup(APP_ID, "RESOURCE_HEALTH"))).thenReturn(false);
	}

	@Test
	void ignoresSingleSampleSpikeInsideSlidingWindow() {
		when(valueOperations.get(keys.metricSnapshot(APP_ID, com.vdt.log_monitoring.modules.anomaly.internal.rule.AnomalyMetricRule.CPU_USAGE)))
			.thenReturn(snapshot("[20.0,22.0,91.0,23.0,21.0,20.0]", 20.0));

		job.detect();

		verify(anomalyFacade, never()).createOrUpdateReport(any());
		verify(anomalyDetectedPublisher, never()).publish(any());
	}

	@Test
	void reportsCriticalWhenTwoOfLastThreeSamplesCrossCriticalThreshold() {
		when(valueOperations.get(keys.metricSnapshot(APP_ID, com.vdt.log_monitoring.modules.anomaly.internal.rule.AnomalyMetricRule.CPU_USAGE)))
			.thenReturn(snapshot("[80.0,86.0,88.0,91.0,92.0,89.0]", 89.0));
		when(anomalyFacade.createOrUpdateReport(any())).thenReturn(report());

		job.detect();

		ArgumentCaptor<AnomalyFacade.CreateAnomalyReportCommand> commandCaptor =
			ArgumentCaptor.forClass(AnomalyFacade.CreateAnomalyReportCommand.class);
		verify(anomalyFacade).createOrUpdateReport(commandCaptor.capture());
		assertThat(commandCaptor.getValue().severity()).isEqualTo("CRITICAL");
		assertThat(commandCaptor.getValue().summary()).contains("2/3 recent samples");
		verify(anomalyDetectedPublisher).publish(any(AnomalyDetectedEvent.class));
	}

	private String snapshot(String recentValues, double current) {
		return """
			{
			  "ruleId": "CPU_USAGE",
			  "current": %s,
			  "avg": 72.0,
			  "max": 92.0,
			  "samples": 6,
			  "recentValues": %s,
			  "lastSeen": "%s"
			}
			""".formatted(current, recentValues, LAST_SEEN);
	}

	private AnomalyFacade.AnomalyReportDto report() {
		return new AnomalyFacade.AnomalyReportDto(
			REPORT_ID,
			APP_ID,
			null,
			"ANOMALY_METRIC",
			"RESOURCE_HEALTH_THRESHOLD",
			"RESOURCE_HEALTH",
			"CRITICAL",
			"OPEN",
			"Resource anomaly detected",
			"CPU_USAGE breached",
			"Resource pressure detected",
			0.95,
			LAST_SEEN,
			LAST_SEEN,
			1,
			LAST_SEEN,
			LAST_SEEN,
			"{}",
			true,
			"SEVERITY_CRITICAL",
			"NOT_REQUESTED",
			null,
			null,
			null,
			null,
			null,
			null,
			LAST_SEEN,
			LAST_SEEN);
	}
}
