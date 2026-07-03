package com.vdt.log_monitoring.api.alerting.dto;

import java.time.Instant;
import java.util.UUID;

import com.vdt.log_monitoring.modules.alerting.api.AlertingFacade;

public record ChatRoomResponse(
	UUID id,
	String channel,
	String name,
	String chatId,
	String description,
	String status,
	UUID createdBy,
	Instant createdAt,
	Instant updatedAt
) {

	public static ChatRoomResponse from(AlertingFacade.ChatRoomDto chatRoom) {
		return new ChatRoomResponse(
			chatRoom.id(),
			chatRoom.channel(),
			chatRoom.name(),
			chatRoom.chatId(),
			chatRoom.description(),
			chatRoom.status(),
			chatRoom.createdBy(),
			chatRoom.createdAt(),
			chatRoom.updatedAt()
		);
	}
}
