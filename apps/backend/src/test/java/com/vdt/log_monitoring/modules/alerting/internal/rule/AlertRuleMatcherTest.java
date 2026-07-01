package com.vdt.log_monitoring.modules.alerting.internal.rule;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class AlertRuleMatcherTest {

	private static final Instant MATCH_TIME = Instant.parse("2026-06-18T03:00:00Z");

	private final AlertRuleMatcher matcher = new AlertRuleMatcher(
		new AlertRuleTimeWindowMatcher(ZoneOffset.UTC));

	@Test
	void matchesMinimumSeverityAndKeywordCaseInsensitively() {
		AlertRuleDefinition rule = rule(AlertSeverity.ERROR, "Payment Failed");

		assertThat(matcher.matches(rule, AlertSeverity.CRITICAL, "payment failed at checkout", MATCH_TIME)).isTrue();
		assertThat(matcher.matches(rule, AlertSeverity.WARN, "payment failed at checkout", MATCH_TIME)).isFalse();
		assertThat(matcher.matches(rule, AlertSeverity.ERROR, "database timeout", MATCH_TIME)).isFalse();
	}

	@Test
	void blankKeywordMatchesAnyMessage() {
		assertThat(matcher.matches(rule(AlertSeverity.ERROR, null), AlertSeverity.ERROR, null, MATCH_TIME)).isTrue();
	}

	@Test
	void activeWindowLimitsRuleMatching() {
		AlertRuleDefinition rule = rule(
			AlertSeverity.WARN,
			null,
			LocalTime.parse("00:00"),
			LocalTime.parse("06:00"));

		assertThat(matcher.matches(rule, AlertSeverity.WARN, "payment failed", MATCH_TIME)).isTrue();
		assertThat(matcher.matches(
			rule,
			AlertSeverity.WARN,
			"payment failed",
			Instant.parse("2026-06-18T10:00:00Z"))).isFalse();
	}

	@Test
	void overnightActiveWindowMatchesAcrossMidnight() {
		AlertRuleDefinition rule = rule(
			AlertSeverity.WARN,
			null,
			LocalTime.parse("18:00"),
			LocalTime.parse("08:00"));

		assertThat(matcher.matches(
			rule,
			AlertSeverity.WARN,
			"payment failed",
			Instant.parse("2026-06-18T20:00:00Z"))).isTrue();
		assertThat(matcher.matches(
			rule,
			AlertSeverity.WARN,
			"payment failed",
			Instant.parse("2026-06-18T02:00:00Z"))).isTrue();
		assertThat(matcher.matches(
			rule,
			AlertSeverity.WARN,
			"payment failed",
			Instant.parse("2026-06-18T12:00:00Z"))).isFalse();
	}

	private AlertRuleDefinition rule(AlertSeverity severity, String keyword) {
		return rule(severity, keyword, null, null);
	}

	private AlertRuleDefinition rule(
		AlertSeverity severity,
		String keyword,
		LocalTime activeStartTime,
		LocalTime activeEndTime
	) {
		return AlertRuleDefinition.from(AlertRuleEntity.create(
			UUID.randomUUID(), "Rule", null, severity, AlertSeverity.CRITICAL, keyword, 1, 60, 60,
			activeStartTime, activeEndTime,
			AlertRuleEntity.channelOnlyTargets(Set.of(AlertChannel.WEBSOCKET)), UUID.randomUUID()));
	}
}
