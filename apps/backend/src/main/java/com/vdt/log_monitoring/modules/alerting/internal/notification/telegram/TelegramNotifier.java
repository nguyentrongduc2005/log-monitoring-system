package com.vdt.log_monitoring.modules.alerting.internal.notification.telegram;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.vdt.log_monitoring.modules.alerting.internal.alert.AlertEntity;
import com.vdt.log_monitoring.modules.alerting.internal.notification.ChatRoomEntity;
import com.vdt.log_monitoring.modules.alerting.internal.notification.ChatRoomRepository;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertRuleDefinition;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertRuleDefinition.DeliveryTarget;

@Slf4j
@Component
public class TelegramNotifier {
	private static final DateTimeFormatter TELEGRAM_TIME = DateTimeFormatter
		.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'")
		.withZone(ZoneOffset.UTC);
	private static final int MAX_LABEL_LENGTH = 240;
	private static final int MAX_KEYWORD_LENGTH = 320;
	private static final int MAX_LOG_MESSAGE_LENGTH = 1800;
	private static final int SHORT_IDENTIFIER_LENGTH = 12;

	private final RestClient restClient;
	private final ChatRoomRepository chatRoomRepository;
	private final String botToken;

	public TelegramNotifier(
		RestClient.Builder restClientBuilder,
		ChatRoomRepository chatRoomRepository,
		@Value("${app.alerting.telegram.api-base-url}") String apiBaseUrl,
		@Value("${app.alerting.telegram.bot-token}") String botToken
	) {
		this.restClient = restClientBuilder.baseUrl(apiBaseUrl).build();
		this.chatRoomRepository = chatRoomRepository;
		this.botToken = botToken;
	}

	public void notify(AlertEntity alert, AlertRuleDefinition rule, DeliveryTarget target) {
		String targetChatId = resolveChatId(target);
		if (isBlank(botToken) || isBlank(targetChatId)) {
			log.info(
					"Telegram alert skipped because bot token is missing or chat room cannot be resolved alertId={} ruleId={}",
					alert.getId(),
					rule.id());
			return;
		}

		try {
			restClient.post()
					.uri("/bot{token}/sendMessage", botToken)
					.body(Map.of(
							"chat_id", targetChatId,
							"text", formatMessage(alert, rule),
							"parse_mode", "HTML",
							"disable_web_page_preview", true))
					.retrieve()
					.toBodilessEntity();
		} catch (RuntimeException exception) {
			log.warn(
					"Telegram alert notification failed alertId={} ruleId={}",
					alert.getId(),
					rule.id(),
					exception);
		}
	}

	private String resolveChatId(DeliveryTarget target) {
		if (target == null || target.chatRoomId() == null) {
			return null;
		}
		return chatRoomRepository.findById(target.chatRoomId())
				.map(ChatRoomEntity::getChatId)
				.orElse(null);
	}

	String formatMessage(AlertEntity alert, AlertRuleDefinition rule) {
		String displayName = alert.getApplicationDisplayName() == null
			? alert.getApplicationName()
			: alert.getApplicationDisplayName();
		String keyword = isBlank(rule.keywordPattern())
			? "Any message"
			: "Contains “%s”".formatted(rule.keywordPattern());

		return """
				%s <b>%s ALERT</b>

				<b>Application:</b> %s
				<b>Service:</b> <code>%s</code>
				<b>Rule:</b> %s
				<b>Match:</b> Severity ≥ %s · %s
				<b>Threshold:</b> %d events / %ds
				<b>Occurrences:</b> %d
				<b>First seen:</b> %s
				<b>Last seen:</b> %s

				<b>Log message</b>
				<pre>%s</pre>

				<code>Alert %s · Fingerprint %s</code>
				""".formatted(
				severityIcon(alert),
				alert.getSeverity().name(),
				escapeHtml(displayName, MAX_LABEL_LENGTH),
				escapeHtml(alert.getApplicationName(), MAX_LABEL_LENGTH),
				escapeHtml(rule.name(), MAX_LABEL_LENGTH),
				rule.minSeverity().name(),
				escapeHtml(keyword, MAX_KEYWORD_LENGTH),
				rule.thresholdCount(),
				rule.thresholdWindowSeconds(),
				alert.getOccurrenceCount(),
				TELEGRAM_TIME.format(alert.getFirstSeenAt()),
				TELEGRAM_TIME.format(alert.getLastSeenAt()),
				escapeHtml(alert.getMessage(), MAX_LOG_MESSAGE_LENGTH),
				shorten(alert.getId().toString()),
				shorten(alert.getFingerprint())).trim();
	}

	private String severityIcon(AlertEntity alert) {
		return switch (alert.getSeverity()) {
			case INFO -> "ℹ️";
			case WARN -> "⚠️";
			case ERROR -> "🚨";
			case CRITICAL -> "🔥";
		};
	}

	private String escapeHtml(String value, int maxLength) {
		StringBuilder escaped = new StringBuilder(Math.min(value.length(), maxLength));
		for (int index = 0; index < value.length(); index++) {
			String replacement = switch (value.charAt(index)) {
				case '&' -> "&amp;";
				case '<' -> "&lt;";
				case '>' -> "&gt;";
				default -> String.valueOf(value.charAt(index));
			};
			if (escaped.length() + replacement.length() > maxLength - 1) {
				escaped.append('…');
				break;
			}
			escaped.append(replacement);
		}
		return escaped.toString();
	}

	private String shorten(String value) {
		return value.length() <= SHORT_IDENTIFIER_LENGTH
			? value
			: value.substring(0, SHORT_IDENTIFIER_LENGTH);
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
