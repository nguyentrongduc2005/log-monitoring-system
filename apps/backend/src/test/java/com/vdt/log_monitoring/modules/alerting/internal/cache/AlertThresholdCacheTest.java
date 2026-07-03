package com.vdt.log_monitoring.modules.alerting.internal.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.util.ReflectionTestUtils;

import com.vdt.log_monitoring.modules.alerting.internal.cache.AlertThresholdCache.DecisionType;
import com.vdt.log_monitoring.modules.alerting.internal.cache.AlertThresholdCache.ThresholdDecision;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertChannel;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertRuleDefinition;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertRuleEntity;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertSeverity;

class AlertThresholdCacheTest {

	private final StringRedisTemplate redisTemplate = mock();
	private final AlertThresholdCache cache = new AlertThresholdCache(redisTemplate);

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(cache, "keyPrefix", "alerting:evaluation:");
		ReflectionTestUtils.setField(cache, "idempotencyTtlSeconds", 86_400L);
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	void mapsAtomicRedisResultToTriggeredDecision() {
		Instant firstSeenAt = Instant.parse("2026-06-18T03:59:00Z");
		when(redisTemplate.execute(
			any(RedisScript.class),
			anyList(),
			any(Object[].class)))
			.thenReturn(List.of(2L, 3L, firstSeenAt.toEpochMilli()));

		ThresholdDecision decision = cache.evaluate(
			rule(), UUID.randomUUID(), UUID.randomUUID(), Instant.parse("2026-06-18T04:00:00Z"));

		assertThat(decision.type()).isEqualTo(DecisionType.TRIGGERED);
		assertThat(decision.count()).isEqualTo(3);
		assertThat(decision.firstSeenAt()).isEqualTo(firstSeenAt);
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	void mapsRepeatedKafkaEventToDuplicateDecision() {
		when(redisTemplate.execute(
			any(RedisScript.class),
			anyList(),
			any(Object[].class)))
			.thenReturn(List.of(-1L, 0L, 0L));

		ThresholdDecision decision = cache.evaluate(
			rule(), UUID.randomUUID(), UUID.randomUUID(), Instant.parse("2026-06-18T04:00:00Z"));

		assertThat(decision.type()).isEqualTo(DecisionType.DUPLICATE);
		assertThat(decision.count()).isZero();
	}

	private AlertRuleDefinition rule() {
		return AlertRuleDefinition.from(AlertRuleEntity.create(
			UUID.fromString("00000000-0000-0000-0000-000000000101"),
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
