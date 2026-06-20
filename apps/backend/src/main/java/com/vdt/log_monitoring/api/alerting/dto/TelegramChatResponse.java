package com.vdt.log_monitoring.api.alerting.dto;

import com.vdt.log_monitoring.modules.alerting.api.AlertingFacade;

public record TelegramChatResponse(
	String chatId,
	String name,
	String type,
	String username
) {

	public static TelegramChatResponse from(AlertingFacade.TelegramChatDto chat) {
		return new TelegramChatResponse(chat.chatId(), chat.name(), chat.type(), chat.username());
	}
}
