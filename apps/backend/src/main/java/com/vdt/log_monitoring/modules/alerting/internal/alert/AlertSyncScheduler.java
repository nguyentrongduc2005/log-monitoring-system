package com.vdt.log_monitoring.modules.alerting.internal.alert;

import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertSyncScheduler {

	private final StringRedisTemplate redisTemplate;
	private final AlertRepository alertRepository;

	@Value("${app.alerting.threshold-cache.key-prefix}")
	private String keyPrefix;

	@Scheduled(fixedDelayString = "${app.alerting.sync-interval-ms:5000}")
	@Transactional
	public void syncDirtyAlerts() {
		String dirtySetKey = keyPrefix + "dirty_alerts";
		
		while (true) {
			String scopeKey = redisTemplate.opsForSet().pop(dirtySetKey);
			if (scopeKey == null) {
				break;
			}
			
			try {
				syncAlertScope(scopeKey);
			} catch (Exception exception) {
				log.error("Failed to sync alert scope: {}", scopeKey, exception);
			}
		}
	}

	private void syncAlertScope(String scopeKey) {
		Object countObj = redisTemplate.opsForHash().get(scopeKey, "count");
		Object lastSeenAtObj = redisTemplate.opsForHash().get(scopeKey, "lastSeenAt");
		
		if (countObj == null || lastSeenAtObj == null) {
			return;
		}
		
		long count = Long.parseLong(countObj.toString());
		long lastSeenAtMillis = Long.parseLong(lastSeenAtObj.toString());
		Instant lastSeenAt = Instant.ofEpochMilli(lastSeenAtMillis);
		
		String[] parts = scopeKey.split(":");
		if (parts.length < 2) {
			return;
		}
		
		String applicationIdStr = parts[parts.length - 1];
		String ruleIdStr = parts[parts.length - 2];
		
		UUID ruleId;
		UUID applicationId;
		try {
			ruleId = UUID.fromString(ruleIdStr);
			applicationId = UUID.fromString(applicationIdStr);
		} catch (IllegalArgumentException exception) {
			log.warn("Invalid UUID in scope key {}: {}", scopeKey, exception.getMessage());
			return;
		}
		
		alertRepository.updateOccurrenceCountAndLastSeenAt(
				ruleId, 
				applicationId, 
				count, 
				lastSeenAt, 
				Instant.now());
	}
}
