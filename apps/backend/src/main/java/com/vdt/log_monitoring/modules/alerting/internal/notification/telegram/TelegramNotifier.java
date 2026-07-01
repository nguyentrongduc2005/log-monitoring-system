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
import com.vdt.log_monitoring.modules.anomaly.api.AnomalyFacade;
import com.vdt.log_monitoring.modules.identity.api.ApplicationAccessFacade;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

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
	private final ObjectMapper objectMapper;
	private final ApplicationAccessFacade applicationAccessFacade;
	private final String botToken;

	public TelegramNotifier(
		RestClient.Builder restClientBuilder,
		ChatRoomRepository chatRoomRepository,
		ObjectMapper objectMapper,
		ApplicationAccessFacade applicationAccessFacade,
		@Value("${app.alerting.telegram.api-base-url}") String apiBaseUrl,
		@Value("${app.alerting.telegram.bot-token}") String botToken
	) {
		this.restClient = restClientBuilder.baseUrl(apiBaseUrl).build();
		this.chatRoomRepository = chatRoomRepository;
		this.objectMapper = objectMapper;
		this.applicationAccessFacade = applicationAccessFacade;
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

		sendToTelegram(targetChatId, formatMessage(alert, rule), alert.getId().toString());
	}

	public void notifyAnomalyAlert(AlertEntity alert, String targetChatId) {
		if (isBlank(botToken) || isBlank(targetChatId)) {
			log.info("Telegram anomaly alert skipped because bot token is missing or chat room cannot be resolved alertId={}", alert.getId());
			return;
		}
		sendToTelegram(targetChatId, formatAnomalyMessage(alert), alert.getId().toString());
	}

	public void notifyAnomalyAiReport(AnomalyFacade.AnomalyReportDto report, String targetChatId) {
		if (isBlank(botToken) || isBlank(targetChatId)) {
			log.info("Telegram AI report skipped because bot token is missing or chat room cannot be resolved reportId={}", report.id());
			return;
		}
		sendToTelegram(targetChatId, formatAiReportMessage(report), report.id().toString());
	}

	private void sendToTelegram(String targetChatId, String text, String referenceId) {
		try {
			restClient.post()
					.uri("/bot{token}/sendMessage", botToken)
					.body(Map.of(
							"chat_id", targetChatId,
							"text", text,
							"parse_mode", "HTML",
							"disable_web_page_preview", true))
					.retrieve()
					.toBodilessEntity();
		} catch (RuntimeException exception) {
			log.warn("Telegram notification failed refId={}", referenceId, exception);
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

		java.util.List<com.vdt.log_monitoring.modules.alerting.internal.alert.AlertLogSample> samples = alert.getLogSamples();
		String logMessagesText = samples == null || samples.isEmpty()
			? "<i>No log samples available</i>"
			: samples.stream()
				.limit(2)
				.map(msg -> "<pre>[" + msg.level() + "] " + escapeHtml(msg.message(), MAX_LOG_MESSAGE_LENGTH / 2) + "</pre>")
				.collect(java.util.stream.Collectors.joining("\n"));

		return """
				%s <b>%s ALERT: %s</b>
				━━━━━━━━━━━━━━━━━━━━━
				🎯 <b>App:</b> %s (<code>%s</code>)
				📊 <b>Count:</b> %d times
				⏱️ <b>Threshold:</b> %d events / %ds
				🔍 <b>Match:</b> Severity ≥ %s | %s
				
				🕒 <b>First:</b> %s
				🕒 <b>Last:</b> %s
				
				📝 <b>Log Samples (Top %d):</b>
				%s
				━━━━━━━━━━━━━━━━━━━━━
				🆔 <code>Alert ID: %s</code>
				""".formatted(
				severityIcon(alert),
				alert.getSeverity().name(),
				escapeHtml(rule.name(), MAX_LABEL_LENGTH),
				escapeHtml(displayName, MAX_LABEL_LENGTH),
				escapeHtml(alert.getApplicationName(), MAX_LABEL_LENGTH),
				alert.getOccurrenceCount(),
				rule.thresholdCount(),
				rule.thresholdWindowSeconds(),
				rule.minSeverity().name(),
				escapeHtml(keyword, MAX_KEYWORD_LENGTH),
				TELEGRAM_TIME.format(alert.getFirstSeenAt()),
				TELEGRAM_TIME.format(alert.getLastSeenAt()),
				alert.getLogSamples() != null ? alert.getLogSamples().size() : 0,
				logMessagesText,
				shorten(alert.getId().toString())).trim();
	}

	private String formatAnomalyMessage(AlertEntity alert) {
		String displayName = alert.getApplicationDisplayName() == null
			? alert.getApplicationName()
			: alert.getApplicationDisplayName();

		java.util.List<com.vdt.log_monitoring.modules.alerting.internal.alert.AlertLogSample> samples = alert.getLogSamples();
		String logMessagesText = samples == null || samples.isEmpty()
			? "<i>No log samples available</i>"
			: samples.stream()
				.limit(2)
				.map(msg -> "<pre>[" + msg.level() + "] " + escapeHtml(msg.message(), MAX_LOG_MESSAGE_LENGTH / 2) + "</pre>")
				.collect(java.util.stream.Collectors.joining("\n"));

		return """
				%s <b>ANOMALY DETECTED</b>
				━━━━━━━━━━━━━━━━━━━━━
				🎯 <b>App:</b> %s
				🔥 <b>Rule:</b> %s
				📊 <b>Count:</b> %d times
				
				🕒 <b>First:</b> %s
				🕒 <b>Last:</b> %s
				
				📝 <b>Log Samples:</b>
				%s
				━━━━━━━━━━━━━━━━━━━━━
				🆔 <code>Alert ID: %s</code>
				""".formatted(
				severityIcon(alert),
				escapeHtml(displayName, MAX_LABEL_LENGTH),
				escapeHtml(alert.getRuleName(), MAX_LABEL_LENGTH),
				alert.getOccurrenceCount(),
				TELEGRAM_TIME.format(alert.getFirstSeenAt()),
				TELEGRAM_TIME.format(alert.getLastSeenAt()),
				logMessagesText,
				shorten(alert.getId().toString())).trim();
	}

	private String formatAiReportMessage(AnomalyFacade.AnomalyReportDto report) {
		String summary = "";
		String likelyCause = "";
		String suggestedActions = "";
		String appName = report.applicationId().toString();
		try {
			var app = applicationAccessFacade.findApplicationById(report.applicationId());
			appName = app.displayName() != null && !app.displayName().isBlank() ? app.displayName() : app.name();
		} catch (Exception e) {
			// fallback to UUID
		}
		
		if (report.aiResultJson() != null && !report.aiResultJson().isBlank()) {
			try {
				JsonNode node = objectMapper.readTree(report.aiResultJson());
				summary = node.has("summary") ? node.get("summary").asText() : "";
				likelyCause = node.has("likelyCause") ? node.get("likelyCause").asText() : "";
				
				if (node.has("suggestedActions") && node.get("suggestedActions").isArray()) {
					StringBuilder actionsBuilder = new StringBuilder();
					for (JsonNode action : node.get("suggestedActions")) {
						actionsBuilder.append("- ").append(action.asText()).append("\n");
					}
					suggestedActions = actionsBuilder.toString();
				}
			} catch (Exception e) {
				log.warn("Failed to parse AI result JSON", e);
			}
		}

		return """
				🤖 <b>AI ANOMALY REPORT</b>
				━━━━━━━━━━━━━━━━━━━━━
				🎯 <b>App:</b> <code>%s</code>
				🔥 <b>Rule:</b> %s

				🧠 <b>Tóm tắt:</b>
				%s

				💡 <b>Nguyên nhân khả thi:</b>
				%s

				🛠 <b>Đề xuất xử lý:</b>
				%s
				━━━━━━━━━━━━━━━━━━━━━
				🆔 <code>Report ID: %s</code>
				""".formatted(
				escapeHtml(appName, MAX_LABEL_LENGTH),
				escapeHtml(report.ruleName(), MAX_LABEL_LENGTH),
				escapeHtml(summary, MAX_LOG_MESSAGE_LENGTH),
				escapeHtml(likelyCause, MAX_LOG_MESSAGE_LENGTH),
				escapeHtml(suggestedActions, MAX_LOG_MESSAGE_LENGTH),
				shorten(report.id().toString())).trim();
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
