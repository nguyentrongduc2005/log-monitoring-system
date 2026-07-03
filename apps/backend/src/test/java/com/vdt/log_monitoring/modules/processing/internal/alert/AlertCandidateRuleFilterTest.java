package com.vdt.log_monitoring.modules.processing.internal.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdt.log_monitoring.modules.processing.internal.model.LogFingerprint;
import com.vdt.log_monitoring.modules.processing.internal.model.LogLevel;
import com.vdt.log_monitoring.modules.processing.internal.model.LogMetadata;
import com.vdt.log_monitoring.modules.processing.internal.model.LogProcessingStatus;
import com.vdt.log_monitoring.modules.processing.internal.model.ProcessedLog;

class AlertCandidateRuleFilterTest {

	private static final UUID APPLICATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");

	private final StringRedisTemplate redisTemplate = org.mockito.Mockito.mock();
	private final ValueOperations<String, String> valueOperations = org.mockito.Mockito.mock();
	private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
	private final AlertCandidateRuleFilter filter = new AlertCandidateRuleFilter(redisTemplate, objectMapper);

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(filter, "keyPrefix", "alerting:rules:active:");
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
	}

	@Test
	void matchesWarnLogWhenCachedRuleMatchesKeywordAndTimeWindow() throws Exception {
		when(valueOperations.get("alerting:rules:active:" + APPLICATION_ID))
			.thenReturn(objectMapper.writeValueAsString(List.of(Map.of(
				"minSeverity", "WARN",
				"keywordPattern", "payment_failed",
				"activeStartTime", "00:00",
				"activeEndTime", "06:00"
			))));

		assertThat(filter.matches(log(LogLevel.WARN, "payment_failed from gateway", "2026-06-18T03:00:00Z")))
			.isTrue();
		assertThat(filter.matches(log(LogLevel.WARN, "payment_failed from gateway", "2026-06-18T10:00:00Z")))
			.isFalse();
	}

	@Test
	void fallsBackToCriticalLevelsWhenCacheMisses() {
		when(valueOperations.get("alerting:rules:active:" + APPLICATION_ID)).thenReturn(null);

		assertThat(filter.matches(log(LogLevel.INFO, "payment_failed from gateway", "2026-06-18T03:00:00Z")))
			.isFalse();
		assertThat(filter.matches(log(LogLevel.ERROR, "payment_failed from gateway", "2026-06-18T03:00:00Z")))
			.isTrue();
	}

	private ProcessedLog log(LogLevel level, String message, String timestamp) {
		Instant instant = Instant.parse(timestamp);
		return new ProcessedLog(
			UUID.randomUUID(),
			UUID.randomUUID(),
			APPLICATION_ID,
			"checkout-api",
			"Checkout API",
			level,
			message,
			null,
			instant,
			instant,
			instant,
			new LogFingerprint("checkout-payment"),
			LogProcessingStatus.STORED,
			LogMetadata.empty()
		);
	}
}
