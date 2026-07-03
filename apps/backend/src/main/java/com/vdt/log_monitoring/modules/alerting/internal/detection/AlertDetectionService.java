package com.vdt.log_monitoring.modules.alerting.internal.detection;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.vdt.log_monitoring.modules.alerting.internal.evaluation.AlertEvaluationCandidate;
import com.vdt.log_monitoring.modules.alerting.internal.evaluation.AlertEvaluationService;
import com.vdt.log_monitoring.modules.processing.api.events.CriticalLogDetectedEvent;

@Service
@RequiredArgsConstructor
public class AlertDetectionService {

	private final AlertEvaluationService alertEvaluationService;

	public void detect(CriticalLogDetectedEvent event) {
		alertEvaluationService.evaluate(new AlertEvaluationCandidate(
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
