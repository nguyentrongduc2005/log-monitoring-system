package com.vdt.log_monitoring.modules.alerting.internal.notification;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vdt.log_monitoring.modules.alerting.api.AlertingException;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertChannel;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

	private final ChatRoomRepository chatRoomRepository;

	@Transactional
	public ChatRoomEntity createChatRoom(
		String channel,
		String name,
		String chatId,
		String description,
		UUID createdBy
	) {
		AlertChannel parsedChannel = parseChannel(channel);
		String normalizedChatId = requireText(chatId, "chatId");
		if (chatRoomRepository.existsByChannelAndChatId(parsedChannel, normalizedChatId)) {
			throw new AlertingException(
				AlertingException.ErrorCode.CHAT_ROOM_ALREADY_EXISTS,
				"Chat room is already registered for this channel"
			);
		}
		return chatRoomRepository.save(ChatRoomEntity.create(
			parsedChannel,
			requireText(name, "name"),
			normalizedChatId,
			description,
			createdBy
		));
	}

	@Transactional
	public ChatRoomEntity changeStatus(UUID chatRoomId, String status) {
		ChatRoomEntity chatRoom = getChatRoomById(chatRoomId);
		chatRoom.changeStatus(parseStatus(status));
		return chatRoom;
	}

	@Transactional(readOnly = true)
	public List<ChatRoomEntity> listChatRooms(String channel, String status) {
		AlertChannel parsedChannel = parseOptionalChannel(channel);
		ChatRoomStatus parsedStatus = parseOptionalStatus(status);
		List<ChatRoomEntity> chatRooms;
		if (parsedChannel != null && parsedStatus != null) {
			chatRooms = chatRoomRepository.findByChannelAndStatus(parsedChannel, parsedStatus);
		} else if (parsedChannel != null) {
			chatRooms = chatRoomRepository.findByChannel(parsedChannel);
		} else if (parsedStatus != null) {
			chatRooms = chatRoomRepository.findByStatus(parsedStatus);
		} else {
			chatRooms = chatRoomRepository.findAll();
		}
		return chatRooms.stream()
			.sorted(Comparator.comparing(ChatRoomEntity::getCreatedAt).reversed())
			.toList();
	}

	@Transactional(readOnly = true)
	public ChatRoomEntity getChatRoomById(UUID chatRoomId) {
		return chatRoomRepository.findById(chatRoomId)
			.orElseThrow(() -> new AlertingException(
				AlertingException.ErrorCode.CHAT_ROOM_NOT_FOUND,
				"Chat room not found"
			));
	}

	private AlertChannel parseOptionalChannel(String channel) {
		return channel == null || channel.isBlank() ? null : parseChannel(channel);
	}

	private AlertChannel parseChannel(String channel) {
		try {
			return AlertChannel.valueOf(requireText(channel, "channel").toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new AlertingException(
				AlertingException.ErrorCode.INVALID_ALERT_CHANNEL,
				"Invalid alert channel"
			);
		}
	}

	private ChatRoomStatus parseOptionalStatus(String status) {
		return status == null || status.isBlank() ? null : parseStatus(status);
	}

	private ChatRoomStatus parseStatus(String status) {
		try {
			return ChatRoomStatus.valueOf(requireText(status, "status").toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new AlertingException(
				AlertingException.ErrorCode.INVALID_CHAT_ROOM,
				"Invalid chat room status"
			);
		}
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new AlertingException(
				AlertingException.ErrorCode.INVALID_CHAT_ROOM,
				fieldName + " must not be blank"
			);
		}
		return value.trim();
	}
}
