package com.vdt.log_monitoring.modules.alerting.internal.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertChannel;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertRuleDefinition;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertRuleEntity;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertSeverity;

class AlertRuleCacheTest {

	private static final UUID APPLICATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");

	private final StringRedisTemplate redisTemplate = mock();
	private final ValueOperations<String, String> valueOperations = mock();
	private final AlertRuleCache cache = new AlertRuleCache(
		redisTemplate,
		new ObjectMapper().findAndRegisterModules());

	@BeforeEach
	void setUp() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		ReflectionTestUtils.setField(cache, "keyPrefix", "alerting:rules:active:");
		ReflectionTestUtils.setField(cache, "ttl", Duration.ofMinutes(5));
	}

	@Test
	void cacheMissLoadsDatabaseAndPopulatesRedis() {
		AlertRuleDefinition rule = rule();
		when(valueOperations.get("alerting:rules:active:" + APPLICATION_ID)).thenReturn(null);

		List<AlertRuleDefinition> result = cache.getActiveRules(APPLICATION_ID, () -> List.of(rule));

		assertThat(result).containsExactly(rule);
		verify(valueOperations).set(
			eq("alerting:rules:active:" + APPLICATION_ID),
			any(String.class),
			eq(Duration.ofMinutes(5)));
	}

	@Test
	void cacheHitReturnsRulesWithoutLoadingDatabaseAgain() {
		AtomicReference<String> cachedJson = new AtomicReference<>();
		when(valueOperations.get("alerting:rules:active:" + APPLICATION_ID))
			.thenReturn(null)
			.thenAnswer(invocation -> cachedJson.get());
		org.mockito.Mockito.doAnswer(invocation -> {
			cachedJson.set(invocation.getArgument(1));
			return null;
		}).when(valueOperations).set(any(), any(), any(Duration.class));
		cache.getActiveRules(APPLICATION_ID, () -> List.of(rule()));
		@SuppressWarnings("unchecked")
		Supplier<List<AlertRuleDefinition>> loader = mock(Supplier.class);

		List<AlertRuleDefinition> result = cache.getActiveRules(APPLICATION_ID, loader);

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().name()).isEqualTo("Payment failures");
		verify(loader, never()).get();
	}

	private AlertRuleDefinition rule() {
		return AlertRuleDefinition.from(AlertRuleEntity.create(
			APPLICATION_ID,
			"Payment failures",
			null,
			AlertSeverity.ERROR,
			AlertSeverity.CRITICAL,
			"payment",
			3,
			60,
			120,
			null,
			null,
			AlertRuleEntity.channelOnlyTargets(Set.of(AlertChannel.WEBSOCKET)),
			UUID.fromString("00000000-0000-0000-0000-000000000102")));
	}
}
