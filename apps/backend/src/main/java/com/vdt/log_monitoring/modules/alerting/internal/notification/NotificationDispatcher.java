package com.vdt.log_monitoring.modules.alerting.internal.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.alerting.internal.alert.AlertEntity;
import com.vdt.log_monitoring.modules.alerting.internal.notification.telegram.TelegramNotifier;
import com.vdt.log_monitoring.modules.alerting.internal.notification.websocket.WebSocketAlertPublisher;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertRuleDefinition;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertRuleDefinition.DeliveryTarget;

@Component
@RequiredArgsConstructor
public class NotificationDispatcher {

	private final TelegramNotifier telegramNotifier;
	private final WebSocketAlertPublisher webSocketAlertPublisher;

	public void dispatch(AlertEntity alert, AlertRuleDefinition rule) {
		for (DeliveryTarget target : rule.deliveryTargets()) {
			switch (target.channel()) {
				case TELEGRAM -> telegramNotifier.notify(alert, rule, target);
				case WEBSOCKET -> webSocketAlertPublisher.publish(alert);
			}
		}
	}
}
