package com.vdt.log_monitoring.modules.alerting.internal.cache;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertRuleDefinition;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertRuleCache {

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	@Value("${app.alerting.rule-cache.key-prefix}")
	private String keyPrefix;

	@Value("${app.alerting.rule-cache.ttl}")
	private Duration ttl;

	public List<AlertRuleDefinition> getActiveRules(
		UUID applicationId,
		Supplier<List<AlertRuleDefinition>> databaseLoader
	) {
		String key = key(applicationId);
		try {
			String cached = redisTemplate.opsForValue().get(key);
			if (cached != null) {
				return deserialize(cached);
			}
		} catch (DataAccessException | JsonProcessingException exception) {
			log.warn("Unable to read alert rule cache applicationId={}", applicationId, exception);
		}

		List<AlertRuleDefinition> rules = databaseLoader.get();
		try {
			redisTemplate.opsForValue().set(key, serialize(rules), ttl);
		} catch (DataAccessException | JsonProcessingException exception) {
			log.warn("Unable to populate alert rule cache applicationId={}", applicationId, exception);
		}
		return rules;
	}

	public void evict(UUID applicationId) {
		try {
			redisTemplate.delete(key(applicationId));
		} catch (DataAccessException exception) {
			log.warn("Unable to evict alert rule cache applicationId={}", applicationId, exception);
		}
	}

	public void evictAfterCommit(UUID applicationId) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			evict(applicationId);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				evict(applicationId);
			}
		});
	}

	private String serialize(List<AlertRuleDefinition> rules) throws JsonProcessingException {
		return objectMapper.writeValueAsString(rules);
	}

	private List<AlertRuleDefinition> deserialize(String value) throws JsonProcessingException {
		return Arrays.asList(objectMapper.readValue(value, AlertRuleDefinition[].class));
	}

	private String key(UUID applicationId) {
		return keyPrefix + applicationId;
	}

}
