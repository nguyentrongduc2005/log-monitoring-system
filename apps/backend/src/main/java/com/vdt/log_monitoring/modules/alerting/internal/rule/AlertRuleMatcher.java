package com.vdt.log_monitoring.modules.alerting.internal.rule;

import java.util.Locale;
import java.time.Instant;

import org.springframework.stereotype.Component;

@Component
public class AlertRuleMatcher {

	private final AlertRuleTimeWindowMatcher timeWindowMatcher;

	public AlertRuleMatcher(AlertRuleTimeWindowMatcher timeWindowMatcher) {
		this.timeWindowMatcher = timeWindowMatcher;
	}

	public boolean matches(AlertRuleDefinition rule, AlertSeverity severity, String message, Instant timestamp) {
		return severity.ordinal() >= rule.minSeverity().ordinal()
			&& matchesKeyword(rule.keywordPattern(), message)
			&& timeWindowMatcher.matches(rule.activeStartTime(), rule.activeEndTime(), timestamp);
	}

	private boolean matchesKeyword(String keywordPattern, String message) {
		if (keywordPattern == null || keywordPattern.isBlank()) {
			return true;
		}
		return message != null
			&& message.toLowerCase(Locale.ROOT).contains(keywordPattern.toLowerCase(Locale.ROOT));
	}
}
