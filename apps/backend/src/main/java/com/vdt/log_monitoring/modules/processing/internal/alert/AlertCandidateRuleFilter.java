package com.vdt.log_monitoring.modules.processing.internal.alert;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdt.log_monitoring.modules.processing.internal.model.LogLevel;
import com.vdt.log_monitoring.modules.processing.internal.model.ProcessedLog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertCandidateRuleFilter {

	private static final TypeReference<List<CachedAlertRule>> RULE_LIST = new TypeReference<>() {};

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	@Value("${app.alerting.rule-cache.key-prefix}")
	private String keyPrefix;

	public boolean matches(ProcessedLog log) {
		if (log.shouldPublishCriticalAlert()) {
			return true;
		}

		List<CachedAlertRule> rules = readCachedRules(log.applicationId());
		if (rules.isEmpty()) {
			return false;
		}

		return rules.stream().anyMatch(rule -> matches(rule, log));
	}

	private List<CachedAlertRule> readCachedRules(UUID applicationId) {
		try {
			String cached = redisTemplate.opsForValue().get(keyPrefix + applicationId);
			if (cached == null || cached.isBlank()) {
				return List.of();
			}
			return objectMapper.readValue(cached, RULE_LIST);
		} catch (DataAccessException | JsonProcessingException exception) {
			log.warn("Unable to read cached alert rules for processing applicationId={}", applicationId, exception);
			return List.of();
		}
	}

	private boolean matches(CachedAlertRule rule, ProcessedLog log) {
		return severityMatches(log.level(), rule.minSeverity())
			&& keywordMatches(log.message(), rule.keywordPattern())
			&& timeWindowMatches(rule.activeStartTime(), rule.activeEndTime(), log.logTimestamp());
	}

	private boolean severityMatches(LogLevel level, String minSeverity) {
		try {
			return level.ordinal() >= LogLevel.from(minSeverity).ordinal();
		} catch (IllegalArgumentException exception) {
			return false;
		}
	}

	private boolean keywordMatches(String message, String keywordPattern) {
		if (keywordPattern == null || keywordPattern.isBlank()) {
			return true;
		}
		return message != null
			&& message.toLowerCase(Locale.ROOT).contains(keywordPattern.toLowerCase(Locale.ROOT));
	}

	private boolean timeWindowMatches(LocalTime activeStartTime, LocalTime activeEndTime, Instant timestamp) {
		if (activeStartTime == null && activeEndTime == null) {
			return true;
		}
		if (activeStartTime == null || activeEndTime == null || activeStartTime.equals(activeEndTime)) {
			return false;
		}

		LocalTime currentTime = timestamp.atZone(ZoneId.systemDefault()).toLocalTime();
		if (activeStartTime.isBefore(activeEndTime)) {
			return !currentTime.isBefore(activeStartTime) && currentTime.isBefore(activeEndTime);
		}
		return !currentTime.isBefore(activeStartTime) || currentTime.isBefore(activeEndTime);
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record CachedAlertRule(
		String minSeverity,
		String keywordPattern,
		LocalTime activeStartTime,
		LocalTime activeEndTime
	) {
		CachedAlertRule {
			Objects.requireNonNull(minSeverity, "minSeverity must not be null");
		}
	}
}
