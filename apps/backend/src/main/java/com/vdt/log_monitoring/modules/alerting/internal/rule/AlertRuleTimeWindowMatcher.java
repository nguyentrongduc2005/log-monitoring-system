package com.vdt.log_monitoring.modules.alerting.internal.rule;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Objects;

import org.springframework.stereotype.Component;

@Component
public class AlertRuleTimeWindowMatcher {

	private final ZoneId zoneId;

	public AlertRuleTimeWindowMatcher() {
		this(ZoneId.systemDefault());
	}

	AlertRuleTimeWindowMatcher(ZoneId zoneId) {
		this.zoneId = Objects.requireNonNull(zoneId, "zoneId must not be null");
	}

	public boolean matches(LocalTime activeStartTime, LocalTime activeEndTime, Instant timestamp) {
		if (activeStartTime == null && activeEndTime == null) {
			return true;
		}
		if (activeStartTime == null || activeEndTime == null || activeStartTime.equals(activeEndTime)) {
			return false;
		}

		LocalTime currentTime = timestamp.atZone(zoneId).toLocalTime();
		if (activeStartTime.isBefore(activeEndTime)) {
			return !currentTime.isBefore(activeStartTime) && currentTime.isBefore(activeEndTime);
		}

		return !currentTime.isBefore(activeStartTime) || currentTime.isBefore(activeEndTime);
	}
}
