package com.vdt.log_monitoring.modules.alerting.internal.rule;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class AlertRuleMatcherTest {

	private final AlertRuleMatcher matcher = new AlertRuleMatcher();

	@Test
	void matchesMinimumSeverityAndKeywordCaseInsensitively() {
		AlertRuleDefinition rule = rule(AlertSeverity.ERROR, "Payment Failed");

		assertThat(matcher.matches(rule, AlertSeverity.CRITICAL, "payment failed at checkout")).isTrue();
		assertThat(matcher.matches(rule, AlertSeverity.WARN, "payment failed at checkout")).isFalse();
		assertThat(matcher.matches(rule, AlertSeverity.ERROR, "database timeout")).isFalse();
	}

	@Test
	void blankKeywordMatchesAnyMessage() {
		assertThat(matcher.matches(rule(AlertSeverity.ERROR, null), AlertSeverity.ERROR, null)).isTrue();
	}

	private AlertRuleDefinition rule(AlertSeverity severity, String keyword) {
		return AlertRuleDefinition.from(AlertRuleEntity.create(
			UUID.randomUUID(), "Rule", null, severity, keyword, 1, 60, 60,
			AlertRuleEntity.channelOnlyTargets(Set.of(AlertChannel.WEBSOCKET)), UUID.randomUUID()));
	}
}
