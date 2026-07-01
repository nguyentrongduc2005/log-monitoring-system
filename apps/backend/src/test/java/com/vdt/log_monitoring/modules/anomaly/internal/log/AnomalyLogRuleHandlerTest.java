package com.vdt.log_monitoring.modules.anomaly.internal.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.vdt.log_monitoring.modules.anomaly.api.AnomalyFacade;
import com.vdt.log_monitoring.modules.anomaly.api.events.AnomalyDetectedEvent;
import com.vdt.log_monitoring.modules.anomaly.internal.config.AnomalyDetectionProperties;
import com.vdt.log_monitoring.modules.anomaly.internal.redis.AnomalyRedisKeys;
import com.vdt.log_monitoring.modules.anomaly.internal.rule.AnomalyLogRule;
import com.vdt.log_monitoring.modules.processing.api.events.AnomalySignalEvent;

class AnomalyLogRuleHandlerTest {

	private static final UUID APP_ID = UUID.fromString("00000000-0000-0000-0000-000000000103");
	private static final UUID LOG_ID = UUID.fromString("00000000-0000-0000-0000-000000000104");
	private static final Instant TIMESTAMP = Instant.parse("2026-06-28T00:00:00Z");

	private final StringRedisTemplate redisTemplate = org.mockito.Mockito.mock();
	private final ValueOperations<String, String> valueOperations = org.mockito.Mockito.mock();
	private final SetOperations<String, String> setOperations = org.mockito.Mockito.mock();
	private final AnomalyRedisKeys keys = new AnomalyRedisKeys();
	private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = org.mockito.Mockito.mock();
	private final com.vdt.log_monitoring.modules.anomaly.api.AnomalyFacade anomalyFacade = org.mockito.Mockito.mock();
	private final com.vdt.log_monitoring.modules.anomaly.internal.publisher.AnomalyDetectedPublisher publisher = org.mockito.Mockito.mock();

	private final AnomalyLogRuleHandler handler = new AnomalyLogRuleHandler(
		new AnomalyLogClassifier(),
		redisTemplate,
		keys,
		new AnomalyDetectionProperties(Duration.ofMinutes(15), null),
		objectMapper,
		anomalyFacade,
		publisher
	);

	@BeforeEach
	void setUp() throws Exception {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(redisTemplate.opsForSet()).thenReturn(setOperations);
		when(objectMapper.writeValueAsString(any()))
			.thenAnswer(invocation -> new com.fasterxml.jackson.databind.ObjectMapper()
				.writeValueAsString(invocation.getArgument(0)));
	}

	@Test
	void incrementsCounterAndIndexesDimensionForMatchedRule() {
		String counterKey = keys.logCounter(APP_ID, AnomalyLogRule.SECURITY_AUTH_FAILURE, "user", "alice");
		String indexKey = keys.logRuleActiveIndex(APP_ID, AnomalyLogRule.SECURITY_AUTH_FAILURE);
		when(valueOperations.increment(counterKey)).thenReturn(1L);

		handler.handle(event("ERROR", "failed login for user=alice"));

		verify(valueOperations).increment(counterKey);
		verify(redisTemplate).expire(counterKey, AnomalyLogRule.SECURITY_AUTH_FAILURE.ttl());
		verify(setOperations).add(indexKey, "user:alice");
		verify(redisTemplate).expire(indexKey, Duration.ofMinutes(15));
	}

	@Test
	void doesNotRefreshCounterTtlAfterFirstIncrement() {
		String counterKey = keys.logCounter(APP_ID, AnomalyLogRule.SECURITY_AUTH_FAILURE, "user", "alice");
		when(valueOperations.increment(counterKey)).thenReturn(2L);

		handler.handle(event("ERROR", "failed login for user=alice"));

		verify(redisTemplate, never()).expire(counterKey, AnomalyLogRule.SECURITY_AUTH_FAILURE.ttl());
	}

	@Test
	void ignoresNonMatchingInfoLog() {
		handler.handle(event("INFO", "normal processing completed"));

		verify(redisTemplate, never()).opsForValue();
		verify(redisTemplate, never()).opsForSet();
	}

	@Test
	void writesStructuredLogSamplesWithOriginalLevelIntoEvidencePayload() {
		String counterKey = keys.logCounter(APP_ID, AnomalyLogRule.RESOURCE_EXHAUSTED, "service", "payments");
		when(valueOperations.increment(counterKey)).thenReturn(1L);
		when(anomalyFacade.createOrUpdateReport(any()))
			.thenAnswer(invocation -> reportFrom(invocation.getArgument(0)));

		handler.handle(event("WARN", "out of memory service=payments"));

		ArgumentCaptor<AnomalyFacade.CreateAnomalyReportCommand> command =
			ArgumentCaptor.forClass(AnomalyFacade.CreateAnomalyReportCommand.class);
		verify(anomalyFacade).createOrUpdateReport(command.capture());
		assertThat(command.getValue().evidencePayloadJson())
			.contains("\"logSamples\"")
			.contains("\"level\":\"WARN\"")
			.contains("\"message\":\"out of memory service=payments\"")
			.doesNotContain("sampleMessages");
	}

	@Test
	void doesNotRequestAiAgainWhenReportAiPreviouslyFailed() {
		String counterKey = keys.logCounter(APP_ID, AnomalyLogRule.SECURITY_AUTH_FAILURE, "user", "alice");
		when(valueOperations.increment(counterKey)).thenReturn(5L);
		when(redisTemplate.hasKey(org.mockito.ArgumentMatchers.anyString())).thenReturn(false);
		when(anomalyFacade.createOrUpdateReport(any()))
			.thenAnswer(invocation -> reportFrom(invocation.getArgument(0), "FAILED"));

		handler.handle(event("ERROR", "failed login for user=alice"));

		ArgumentCaptor<AnomalyDetectedEvent> event =
			ArgumentCaptor.forClass(AnomalyDetectedEvent.class);
		verify(publisher).publish(event.capture());
		assertThat(event.getValue().aiTriggerRequested()).isFalse();
		assertThat(event.getValue().aiTriggerReason()).isNull();
	}

	@Test
	void publishesSuspiciousKeywordAnomalyWhenThresholdIsReached() {
		String counterKey = keys.logCounter(APP_ID, AnomalyLogRule.SUSPICIOUS_KEYWORD, "service", "payments");
		when(valueOperations.increment(counterKey)).thenReturn(
			1L, 2L, 3L, 4L, 5L,
			6L, 7L, 8L, 9L, 10L,
			11L, 12L, 13L, 14L, 15L,
			16L, 17L, 18L, 19L, 20L);
		when(redisTemplate.hasKey(org.mockito.ArgumentMatchers.anyString())).thenReturn(false);
		when(anomalyFacade.createOrUpdateReport(any()))
			.thenAnswer(invocation -> reportFrom(invocation.getArgument(0)));

		for (int index = 0; index < AnomalyLogRule.SUSPICIOUS_KEYWORD.thresholdCount(); index++) {
			handler.handle(event("WARN", "service degraded for payment-service dependency=payment-gateway"));
		}

		ArgumentCaptor<AnomalyFacade.CreateAnomalyReportCommand> command =
			ArgumentCaptor.forClass(AnomalyFacade.CreateAnomalyReportCommand.class);
		verify(anomalyFacade).createOrUpdateReport(command.capture());
		assertThat(command.getValue().ruleName()).isEqualTo("SUSPICIOUS_KEYWORD");
		assertThat(command.getValue().severity()).isEqualTo("WARN");
		assertThat(command.getValue().summary()).contains("Phát hiện 20 sự kiện log khớp điều kiện, vượt quá ngưỡng cho phép là 20");

		ArgumentCaptor<AnomalyDetectedEvent> event =
			ArgumentCaptor.forClass(AnomalyDetectedEvent.class);
		verify(publisher).publish(event.capture());
		assertThat(event.getValue().ruleName()).isEqualTo("SUSPICIOUS_KEYWORD");
		assertThat(event.getValue().severity()).isEqualTo("WARN");
	}

	private AnomalySignalEvent event(String level, String message) {
		return new AnomalySignalEvent(
			APP_ID,
			"checkout-api",
			"Checkout API",
			TIMESTAMP,
			LOG_ID,
			level,
			"TEST",
			"payments",
			message,
			"fingerprint-1",
			"trace-1",
			java.util.Map.of()
		);
	}

	private AnomalyFacade.AnomalyReportDto reportFrom(AnomalyFacade.CreateAnomalyReportCommand command) {
		return reportFrom(command, "NOT_REQUESTED");
	}

	private AnomalyFacade.AnomalyReportDto reportFrom(AnomalyFacade.CreateAnomalyReportCommand command, String aiStatus) {
		return new AnomalyFacade.AnomalyReportDto(
			UUID.fromString("00000000-0000-0000-0000-000000000301"),
			command.applicationId(),
			null,
			command.sourceType(),
			command.ruleName(),
			command.fingerprint(),
			command.severity(),
			"DETECTED",
			command.title(),
			command.summary(),
			command.hypothesis(),
			command.confidenceScore(),
			command.windowStart(),
			command.windowEnd(),
			1,
			command.windowStart(),
			command.windowEnd(),
			command.evidencePayloadJson(),
			command.aiTriggerRequested(),
			command.aiTriggerReason(),
			aiStatus,
			null,
			null,
			null,
			null,
			null,
			null,
			TIMESTAMP,
			TIMESTAMP);
	}
}
