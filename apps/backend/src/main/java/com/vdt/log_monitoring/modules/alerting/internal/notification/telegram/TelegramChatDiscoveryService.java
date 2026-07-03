package com.vdt.log_monitoring.modules.alerting.internal.notification.telegram;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.vdt.log_monitoring.modules.alerting.api.AlertingException;

@Service
public class TelegramChatDiscoveryService {

	private static final String ALLOWED_UPDATES =
		"[\"message\",\"edited_message\",\"my_chat_member\"]";

	private final RestClient restClient;
	private final String botToken;

	public TelegramChatDiscoveryService(
		RestClient.Builder restClientBuilder,
		@Value("${app.alerting.telegram.api-base-url}") String apiBaseUrl,
		@Value("${app.alerting.telegram.bot-token}") String botToken
	) {
		this.restClient = restClientBuilder.baseUrl(apiBaseUrl).build();
		this.botToken = botToken;
	}

	public List<DiscoveredTelegramChat> discoverChats() {
		if (botToken == null || botToken.isBlank()) {
			throw new AlertingException(
				AlertingException.ErrorCode.TELEGRAM_NOT_CONFIGURED,
				"Telegram bot token is not configured");
		}

		TelegramUpdatesResponse response;
		try {
			response = restClient.get()
				.uri(uriBuilder -> uriBuilder
					.path("/bot{token}/getUpdates")
					.queryParam("timeout", 0)
					.queryParam("allowed_updates", ALLOWED_UPDATES)
					.build(botToken))
				.retrieve()
				.body(TelegramUpdatesResponse.class);
		} catch (RestClientException exception) {
			throw discoveryFailed("Failed to query Telegram updates", exception);
		}

		if (response == null || !response.ok()) {
			String description = response == null ? null : response.description();
			throw discoveryFailed(
				description == null || description.isBlank()
					? "Telegram rejected the chat discovery request"
					: "Telegram chat discovery failed: " + description,
				null);
		}

		Map<String, DiscoveredTelegramChat> chatsById = new LinkedHashMap<>();
		for (TelegramUpdate update : response.result() == null ? List.<TelegramUpdate>of() : response.result()) {
			update.chats()
				.filter(this::isSupportedChat)
				.map(this::toDiscoveredChat)
				.forEach(chat -> chatsById.putIfAbsent(chat.chatId(), chat));
		}
		return chatsById.values().stream()
			.sorted(Comparator.comparing(DiscoveredTelegramChat::name, String.CASE_INSENSITIVE_ORDER))
			.toList();
	}

	private boolean isSupportedChat(TelegramChat chat) {
		return chat != null
			&& chat.id() != null
			&& ("group".equalsIgnoreCase(chat.type()) || "supergroup".equalsIgnoreCase(chat.type()));
	}

	private DiscoveredTelegramChat toDiscoveredChat(TelegramChat chat) {
		String username = normalize(chat.username());
		String name = normalize(chat.title());
		if (name == null) {
			name = username == null ? String.valueOf(chat.id()) : "@" + username;
		}
		return new DiscoveredTelegramChat(
			String.valueOf(chat.id()),
			name,
			chat.type().toUpperCase(Locale.ROOT),
			username);
	}

	private String normalize(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private AlertingException discoveryFailed(String message, Exception cause) {
		AlertingException exception = new AlertingException(
			AlertingException.ErrorCode.TELEGRAM_DISCOVERY_FAILED,
			message);
		if (cause != null) {
			exception.initCause(cause);
		}
		return exception;
	}

	public record DiscoveredTelegramChat(String chatId, String name, String type, String username) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record TelegramUpdatesResponse(
		boolean ok,
		List<TelegramUpdate> result,
		String description
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record TelegramUpdate(
		TelegramMessage message,
		@JsonProperty("edited_message") TelegramMessage editedMessage,
		@JsonProperty("my_chat_member") TelegramChatMemberUpdate myChatMember
	) {
		private Stream<TelegramChat> chats() {
			return Stream.of(
				message == null ? null : message.chat(),
				editedMessage == null ? null : editedMessage.chat(),
				myChatMember == null ? null : myChatMember.chat());
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record TelegramMessage(TelegramChat chat) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record TelegramChatMemberUpdate(TelegramChat chat) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record TelegramChat(Long id, String type, String title, String username) {
	}
}
