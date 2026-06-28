package com.vdt.log_monitoring.modules.alerting.internal.detection;

import java.time.Instant;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vdt.log_monitoring.modules.alerting.internal.alert.AlertEntity;
import com.vdt.log_monitoring.modules.alerting.internal.alert.AlertService;
import com.vdt.log_monitoring.modules.alerting.internal.notification.websocket.WebSocketAlertPublisher;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertChannel;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertDeliveryTarget;
import com.vdt.log_monitoring.modules.anomaly.api.AnomalyFacade;
import com.vdt.log_monitoring.modules.anomaly.api.events.AnomalyAiRequestedEvent;
import com.vdt.log_monitoring.modules.anomaly.api.events.AnomalyDetectedEvent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnomalyAlertService {

	private static final Set<AlertDeliveryTarget> DEFAULT_DELIVERY_TARGETS =
		Set.of(AlertDeliveryTarget.channelOnly(AlertChannel.WEBSOCKET));

	private final AlertService alertService;
	private final WebSocketAlertPublisher webSocketAlertPublisher;
	private final AnomalyFacade anomalyFacade;
	private final AnomalyAiRequestedPublisher anomalyAiRequestedPublisher;

	@Transactional
	public AlertEntity createOrUpdateAlert(AnomalyDetectedEvent event) {
		AlertEntity alert = alertService.createFromAnomaly(event, DEFAULT_DELIVERY_TARGETS);
		webSocketAlertPublisher.publish(alert);
		anomalyFacade.markAlerted(event.anomalyReportId(), alert.getId());
		if (event.aiTriggerRequested()) {
			anomalyFacade.markAiPending(event.anomalyReportId(), event.aiTriggerReason());
			anomalyAiRequestedPublisher.publish(new AnomalyAiRequestedEvent(
				event.anomalyReportId(),
				event.applicationId(),
				alert.getId(),
				event.sourceType(),
				event.aiTriggerReason(),
				Instant.now()));
		}
		return alert;
	}
}
