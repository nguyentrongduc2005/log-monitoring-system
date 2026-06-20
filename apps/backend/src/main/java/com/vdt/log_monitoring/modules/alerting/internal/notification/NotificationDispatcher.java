package com.vdt.log_monitoring.modules.alerting.internal.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.alerting.internal.alert.AlertEntity;
import com.vdt.log_monitoring.modules.alerting.internal.notification.telegram.TelegramNotifier;
import com.vdt.log_monitoring.modules.alerting.internal.notification.websocket.WebSocketAlertPublisher;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertChannel;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertDeliveryTarget;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertRuleEntity;

@Component
@RequiredArgsConstructor
public class NotificationDispatcher {

	private final TelegramNotifier telegramNotifier;
	private final WebSocketAlertPublisher webSocketAlertPublisher;

	public void dispatch(AlertEntity alert, AlertRuleEntity rule) {
		for (AlertDeliveryTarget target : rule.getDeliveryTargets()) {
			if (target.getChannel() == AlertChannel.TELEGRAM) {
				telegramNotifier.notify(alert, rule, target);
			}
			if (target.getChannel() == AlertChannel.WEBSOCKET) {
				webSocketAlertPublisher.publish(alert);
			}
		}
	}
}
