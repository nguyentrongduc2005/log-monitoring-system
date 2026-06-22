package com.vdt.log_monitoring.modules.alerting.internal.rule;

import java.util.Locale;

import org.springframework.stereotype.Component;

@Component
public class AlertRuleMatcher {

	public boolean matches(AlertRuleDefinition rule, AlertSeverity severity, String message) {
		return severity.ordinal() >= rule.minSeverity().ordinal()
			&& matchesKeyword(rule.keywordPattern(), message);
	}

	private boolean matchesKeyword(String keywordPattern, String message) {
		if (keywordPattern == null || keywordPattern.isBlank()) {
			return true;
		}
		return message != null
			&& message.toLowerCase(Locale.ROOT).contains(keywordPattern.toLowerCase(Locale.ROOT));
	}
}
