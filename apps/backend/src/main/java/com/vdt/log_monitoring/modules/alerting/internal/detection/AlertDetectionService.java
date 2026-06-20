package com.vdt.log_monitoring.modules.alerting.internal.detection;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.vdt.log_monitoring.modules.alerting.api.AlertingFacade;
import com.vdt.log_monitoring.modules.processing.api.events.CriticalLogDetectedEvent;

@Service
@RequiredArgsConstructor
public class AlertDetectionService {

	private final AlertingFacade alertingFacade;

	public void detect(CriticalLogDetectedEvent event) {
		alertingFacade.evaluate(new AlertingFacade.AlertCandidate(
			event.eventId(),
			event.ingestionId(),
			event.applicationId(),
			event.applicationName(),
			event.applicationDisplayName(),
			event.level(),
			event.message(),
			event.fingerprint(),
			event.logTimestamp()
		));
	}
}
