package com.vdt.log_monitoring.modules.alerting.internal.cache;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertRuleDefinition;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AlertThresholdCache {

	private static final DefaultRedisScript<List> EVALUATE_SCRIPT = new DefaultRedisScript<>("""
		if redis.call('SET', KEYS[1], '1', 'EX', ARGV[4], 'NX') == false then
		  return {-1, 0, 0}
		end
		local count = redis.call('HINCRBY', KEYS[2], 'count', 1)
		if count == 1 then
		  redis.call('HSET', KEYS[2], 'firstSeenAt', ARGV[5])
		  redis.call('EXPIRE', KEYS[2], ARGV[2])
		end
		redis.call('HSET', KEYS[2], 'lastSeenAt', ARGV[5])
		redis.call('SADD', KEYS[4], KEYS[2])
		local firstSeenAt = redis.call('HGET', KEYS[2], 'firstSeenAt')
		if redis.call('EXISTS', KEYS[3]) == 1 then
		  return {1, count, tonumber(firstSeenAt)}
		end
		if count < tonumber(ARGV[1]) then
		  return {0, count, tonumber(firstSeenAt)}
		end
		redis.call('SET', KEYS[3], '1', 'EX', ARGV[3])
		return {2, count, tonumber(firstSeenAt)}
		""", List.class);

	private static final DefaultRedisScript<Long> ROLLBACK_SCRIPT = new DefaultRedisScript<>("""
		if redis.call('DEL', KEYS[1]) == 0 then
		  return 0
		end
		local count = redis.call('HINCRBY', KEYS[2], 'count', -1)
		if count <= 0 then
		  redis.call('DEL', KEYS[2])
		end
		if ARGV[1] == '1' then
		  redis.call('DEL', KEYS[3])
		end
		return 1
		""", Long.class);

	private final StringRedisTemplate redisTemplate;

	@Value("${app.alerting.threshold-cache.key-prefix}")
	private String keyPrefix;

	@Value("${app.alerting.threshold-cache.idempotency-ttl-seconds}")
	private long idempotencyTtlSeconds;

	public ThresholdDecision evaluate(AlertRuleDefinition rule, UUID applicationId, UUID eventId, Instant occurredAt) {
		List<?> result = redisTemplate.execute(
			EVALUATE_SCRIPT,
			keys(rule.id(), applicationId, eventId),
			Integer.toString(rule.thresholdCount()),
			Integer.toString(rule.thresholdWindowSeconds()),
			Integer.toString(rule.cooldownSeconds()),
			Long.toString(idempotencyTtlSeconds),
			Long.toString(occurredAt.toEpochMilli()));

		if (result == null || result.size() != 3) {
			throw new IllegalStateException("Redis returned an invalid alert threshold result");
		}
		int code = ((Number) result.get(0)).intValue();
		long count = ((Number) result.get(1)).longValue();
		long firstSeenAt = ((Number) result.get(2)).longValue();
		return new ThresholdDecision(DecisionType.fromCode(code), count,
			firstSeenAt == 0 ? occurredAt : Instant.ofEpochMilli(firstSeenAt));
	}

	public void rollback(
			AlertRuleDefinition rule,
			UUID applicationId,
			UUID eventId,
			ThresholdDecision decision) {
		if (decision.type() == DecisionType.DUPLICATE) {
			return;
		}
		redisTemplate.execute(
			ROLLBACK_SCRIPT,
			keys(rule.id(), applicationId, eventId),
			decision.type() == DecisionType.TRIGGERED ? "1" : "0");
	}

	private List<String> keys(UUID ruleId, UUID applicationId, UUID eventId) {
		String scope = ruleId + ":" + applicationId;
		return List.of(
			keyPrefix + "event:" + ruleId + ":" + eventId,
			keyPrefix + "window:" + scope,
			keyPrefix + "cooldown:" + scope,
			keyPrefix + "dirty_alerts");
	}

	public enum DecisionType {
		DUPLICATE(-1), BELOW_THRESHOLD(0), COOLDOWN(1), TRIGGERED(2);

		private final int code;

		DecisionType(int code) {
			this.code = code;
		}

		private static DecisionType fromCode(int code) {
			for (DecisionType type : values()) {
				if (type.code == code) {
					return type;
				}
			}
			throw new IllegalStateException("Unknown alert threshold result: " + code);
		}
	}

	public record ThresholdDecision(DecisionType type, long count, Instant firstSeenAt) {
	}
}
