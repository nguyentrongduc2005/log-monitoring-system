package com.vdt.log_monitoring.modules.alerting.internal.notification.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.vdt.log_monitoring.modules.alerting.internal.alert.AlertEntity;
import com.vdt.log_monitoring.modules.alerting.internal.notification.ChatRoomRepository;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertChannel;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertDeliveryTarget;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertRuleDefinition;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertRuleEntity;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertSeverity;

class TelegramNotifierTest {

	@Test
	void formatsReadableHtmlMessageAndEscapesLogContent() {
		UUID applicationId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000002");
		AlertDeliveryTarget target = AlertDeliveryTarget.channelOnly(AlertChannel.TELEGRAM);
		AlertRuleEntity ruleEntity = AlertRuleEntity.create(
			applicationId,
			"Critical payment errors",
			null,
			AlertSeverity.ERROR,
			AlertSeverity.CRITICAL,
			"payment <failed>",
			3,
			60,
			300,
			Set.of(target),
			userId);
		AlertRuleDefinition rule = AlertRuleDefinition.from(ruleEntity);
		Instant occurredAt = Instant.parse("2026-06-22T14:30:00Z");
		AlertEntity alert = AlertEntity.create(
			rule.id(),
			applicationId,
			"payment-service",
			"Payment & Billing",
			rule.name(),
			AlertSeverity.CRITICAL,
			List.of(new com.vdt.log_monitoring.modules.alerting.internal.alert.AlertLogSample("ERROR", "Payment <failed> because amount > balance & gateway timed out")),
			occurredAt.minusSeconds(30),
			occurredAt.minusSeconds(30),
			occurredAt,
			Set.of(target),
			10);
		TelegramNotifier notifier = new TelegramNotifier(
			RestClient.builder(),
			mock(ChatRoomRepository.class),
			"https://api.telegram.test",
			"test-token");

		String message = notifier.formatMessage(alert, rule);

		assertThat(message)
			.contains("🔥 <b>CRITICAL ALERT: Critical payment errors</b>")
			.contains("🎯 <b>App:</b> Payment &amp; Billing (<code>payment-service</code>)")
			.contains("📊 <b>Count:</b> 10 times")
			.contains("⏱️ <b>Threshold:</b> 3 events / 60s")
			.contains("🔍 <b>Match:</b> Severity ≥ ERROR")
			.contains("🕒 <b>First:</b> 2026-06-22 14:29:30 UTC")
			.contains("🕒 <b>Last:</b> 2026-06-22 14:30:00 UTC")
			.contains("📝 <b>Log Samples (Top 1):</b>")
			.contains("<pre>[ERROR] Payment &lt;failed&gt; because amount &gt; balance &amp; gateway timed out</pre>")
			.doesNotContain("Payment <failed>");
	}
}
