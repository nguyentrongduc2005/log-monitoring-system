package com.vdt.log_monitoring.modules.anomaly.internal.log;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

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
		new AnomalyDetectionProperties(Duration.ofMinutes(15), null, null, null),
		objectMapper,
		anomalyFacade,
		publisher
	);

	@BeforeEach
	void setUp() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(redisTemplate.opsForSet()).thenReturn(setOperations);
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
}
