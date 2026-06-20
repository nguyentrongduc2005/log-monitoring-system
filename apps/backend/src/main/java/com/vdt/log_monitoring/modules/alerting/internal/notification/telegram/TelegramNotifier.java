package com.vdt.log_monitoring.modules.alerting.internal.notification.telegram;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.vdt.log_monitoring.modules.alerting.internal.alert.AlertEntity;
import com.vdt.log_monitoring.modules.alerting.internal.notification.ChatRoomEntity;
import com.vdt.log_monitoring.modules.alerting.internal.notification.ChatRoomRepository;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertDeliveryTarget;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertRuleEntity;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramNotifier {

	private final RestClient.Builder restClientBuilder;
	private final ChatRoomRepository chatRoomRepository;

	@Value("${app.alerting.telegram.api-base-url}")
	private String apiBaseUrl;

	@Value("${app.alerting.telegram.bot-token}")
	private String botToken;

	public void notify(AlertEntity alert, AlertRuleEntity rule, AlertDeliveryTarget target) {
		String targetChatId = resolveChatId(target);
		if (isBlank(botToken) || isBlank(targetChatId)) {
			log.info(
					"Telegram alert skipped because bot token is missing or chat room cannot be resolved alertId={} ruleId={}",
					alert.getId(),
					rule.getId());
			return;
		}

		try {
			restClientBuilder.baseUrl(apiBaseUrl).build()
					.post()
					.uri("/bot{token}/sendMessage", botToken)
					.body(Map.of(
							"chat_id", targetChatId,
							"text", formatMessage(alert, rule)))
					.retrieve()
					.toBodilessEntity();
		} catch (RuntimeException exception) {
			log.warn(
					"Telegram alert notification failed alertId={} ruleId={}",
					alert.getId(),
					rule.getId(),
					exception);
		}
	}

	private String resolveChatId(AlertDeliveryTarget target) {
		if (target == null || target.getChatRoomId() == null) {
			return null;
		}
		return chatRoomRepository.findById(target.getChatRoomId())
				.map(ChatRoomEntity::getChatId)
				.orElse(null);
	}

	private String formatMessage(AlertEntity alert, AlertRuleEntity rule) {
		return """
				[%s] %s
				Application: %s
				Rule: %s
				Message: %s
				Fingerprint: %s
				Triggered at: %s
				""".formatted(
				alert.getSeverity().name(),
				alert.getApplicationDisplayName() == null
						? alert.getApplicationName()
						: alert.getApplicationDisplayName(),
				alert.getApplicationName(),
				rule.getName(),
				alert.getMessage(),
				alert.getFingerprint(),
				alert.getTriggeredAt()).trim();
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
