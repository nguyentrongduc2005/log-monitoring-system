package com.vdt.log_monitoring.modules.alerting.internal.notification;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.vdt.log_monitoring.modules.alerting.internal.alert.AlertEntity;
import com.vdt.log_monitoring.modules.alerting.internal.alert.AlertLogSample;
import com.vdt.log_monitoring.modules.alerting.internal.notification.telegram.TelegramNotifier;
import com.vdt.log_monitoring.modules.alerting.internal.notification.websocket.WebSocketAlertPublisher;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertChannel;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertDeliveryTarget;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertRuleEntity;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertRuleDefinition;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertRuleDefinition.DeliveryTarget;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertSeverity;

class NotificationDispatcherTest {

	@Test
	void dispatchesToEveryTelegramChatRoomAndWebSocket() {
		TelegramNotifier telegramNotifier = mock();
		WebSocketAlertPublisher webSocketPublisher = mock();
		NotificationDispatcher dispatcher = new NotificationDispatcher(
			telegramNotifier, webSocketPublisher);
		AlertDeliveryTarget firstTelegram = AlertDeliveryTarget.of(
			AlertChannel.TELEGRAM,
			UUID.fromString("00000000-0000-0000-0000-000000000011"));
		AlertDeliveryTarget secondTelegram = AlertDeliveryTarget.of(
			AlertChannel.TELEGRAM,
			UUID.fromString("00000000-0000-0000-0000-000000000012"));
		AlertDeliveryTarget websocket = AlertDeliveryTarget.channelOnly(AlertChannel.WEBSOCKET);
		AlertRuleEntity ruleEntity = AlertRuleEntity.create(
			UUID.fromString("00000000-0000-0000-0000-000000000001"),
			"Payment failures", null, AlertSeverity.ERROR, AlertSeverity.CRITICAL, "payment", 1, 60, 60,
			null, null,
			Set.of(firstTelegram, secondTelegram, websocket),
			UUID.fromString("00000000-0000-0000-0000-000000000002"));
		AlertRuleDefinition rule = AlertRuleDefinition.from(ruleEntity);
		DeliveryTarget firstTelegramTarget = target(rule, firstTelegram.getChatRoomId());
		DeliveryTarget secondTelegramTarget = target(rule, secondTelegram.getChatRoomId());
		AlertEntity alert = AlertEntity.create(
			rule.id(), rule.applicationId(),
			"checkout-api", "Checkout API", rule.name(), AlertSeverity.ERROR, List.of(new AlertLogSample("ERROR", "Payment failed")),
			Instant.parse("2026-06-18T04:00:00Z"), Instant.parse("2026-06-18T04:00:00Z"), Instant.parse("2026-06-18T04:00:00Z"), rule.toDeliveryTargets(), 1);

		dispatcher.dispatch(alert, rule);

		verify(telegramNotifier).notify(alert, rule, firstTelegramTarget);
		verify(telegramNotifier).notify(alert, rule, secondTelegramTarget);
		verify(webSocketPublisher).publish(alert);
	}

	private DeliveryTarget target(AlertRuleDefinition rule, UUID chatRoomId) {
		return rule.deliveryTargets().stream()
			.filter(target -> chatRoomId.equals(target.chatRoomId()))
			.findFirst()
			.orElseThrow();
	}
}
