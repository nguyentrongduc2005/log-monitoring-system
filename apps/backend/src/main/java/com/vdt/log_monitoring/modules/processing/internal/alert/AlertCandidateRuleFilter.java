package com.vdt.log_monitoring.modules.processing.internal.alert;

import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.alerting.api.AlertingFacade;
import com.vdt.log_monitoring.modules.processing.internal.model.ProcessedLog;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AlertCandidateRuleFilter {

	private final AlertingFacade alertingFacade;

	public boolean matches(ProcessedLog log) {
		if (log.shouldPublishCriticalAlert()) {
			return true;
		}

		return alertingFacade.hasMatchingActiveRuleCandidate(
			log.applicationId(),
			log.level().name(),
			log.message(),
			log.logTimestamp());
	}
}
