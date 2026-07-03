package com.vdt.log_monitoring.modules.alerting.internal.rule;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.alerting.api.AlertingException;
import com.vdt.log_monitoring.modules.alerting.api.AlertingFacade.AlertDeliveryTargetCommand;
import com.vdt.log_monitoring.modules.alerting.internal.notification.ChatRoomEntity;
import com.vdt.log_monitoring.modules.alerting.internal.notification.ChatRoomRepository;
import com.vdt.log_monitoring.modules.alerting.internal.notification.ChatRoomStatus;

@Component
@RequiredArgsConstructor
public class AlertDeliveryTargetResolver {

	private final ChatRoomRepository chatRoomRepository;

	public Set<AlertDeliveryTarget> resolve(
		List<String> channels,
		List<AlertDeliveryTargetCommand> commands
	) {
		try {
			Set<AlertDeliveryTarget> targets = new HashSet<>();
			if (commands != null) {
				for (AlertDeliveryTargetCommand command : commands) {
					if (!targets.add(resolve(command))) {
						throw new IllegalArgumentException("duplicate delivery target");
					}
				}
			}
			addChannelOnlyTargets(channels, targets);
			if (targets.isEmpty()) {
				throw new IllegalArgumentException("at least one delivery target is required");
			}
			return Set.copyOf(targets);
		} catch (IllegalArgumentException exception) {
			throw new AlertingException(
				AlertingException.ErrorCode.INVALID_CHAT_ROOM,
				"Invalid alert delivery target");
		}
	}

	private void addChannelOnlyTargets(List<String> channels, Set<AlertDeliveryTarget> targets) {
		if (channels == null) {
			return;
		}
		for (String channelValue : channels) {
			AlertChannel channel = parseChannel(channelValue);
			boolean configured = targets.stream().anyMatch(target -> target.getChannel() == channel);
			if (!configured) {
				targets.add(AlertDeliveryTarget.channelOnly(channel));
			}
		}
	}

	private AlertDeliveryTarget resolve(AlertDeliveryTargetCommand command) {
		if (command == null) {
			throw new IllegalArgumentException("delivery target must not be null");
		}
		AlertChannel channel = parseChannel(command.channel());
		if (channel == AlertChannel.WEBSOCKET) {
			return AlertDeliveryTarget.channelOnly(channel);
		}
		UUID chatRoomId = Objects.requireNonNull(command.chatRoomId(), "chatRoomId must not be null");
		ChatRoomEntity chatRoom = chatRoomRepository.findById(chatRoomId)
			.orElseThrow(() -> new AlertingException(
				AlertingException.ErrorCode.CHAT_ROOM_NOT_FOUND,
				"Chat room not found"));
		if (chatRoom.getStatus() != ChatRoomStatus.ACTIVE || chatRoom.getChannel() != channel) {
			throw new IllegalArgumentException("chat room does not match channel");
		}
		return AlertDeliveryTarget.of(channel, chatRoomId);
	}

	private AlertChannel parseChannel(String channel) {
		if (channel == null || channel.isBlank()) {
			throw new IllegalArgumentException("channel must not be blank");
		}
		return AlertChannel.valueOf(channel.trim().toUpperCase(Locale.ROOT));
	}
}
