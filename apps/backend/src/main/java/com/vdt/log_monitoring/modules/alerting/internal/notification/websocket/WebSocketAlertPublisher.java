package com.vdt.log_monitoring.modules.alerting.internal.notification.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.alerting.internal.alert.AlertEntity;
import com.vdt.log_monitoring.modules.realtime.api.RealtimeFacade;
import com.vdt.log_monitoring.modules.realtime.api.events.AlertNotificationMessage;

@Component
@RequiredArgsConstructor
public class WebSocketAlertPublisher {

	private final RealtimeFacade realtimeFacade;

	public void publish(AlertEntity alert) {
		realtimeFacade.publishAlertNotification(new AlertNotificationMessage(
				alert.getId(),
				alert.getRuleId(),
				alert.getRuleName(),
				alert.getApplicationId(),
				alert.getApplicationName(),
				alert.getApplicationDisplayName(),
				alert.getSeverity().name(),
				alert.getLogSamples().stream().map(sample -> new com.vdt.log_monitoring.api.alerting.dto.AlertLogSampleDto(sample.level(), sample.message())).toList(),
				alert.getTriggeredAt()));
	}
}
