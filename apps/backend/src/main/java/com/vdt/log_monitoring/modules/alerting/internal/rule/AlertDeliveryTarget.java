package com.vdt.log_monitoring.modules.alerting.internal.rule;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AlertDeliveryTarget {

	@Enumerated(EnumType.STRING)
	@Column(name = "channel", nullable = false, length = 32)
	private AlertChannel channel;

	@Column(name = "chat_room_id")
	private UUID chatRoomId;

	public static AlertDeliveryTarget of(AlertChannel channel, UUID chatRoomId) {
		return new AlertDeliveryTarget(
			Objects.requireNonNull(channel, "channel must not be null"),
			chatRoomId
		);
	}

	public static AlertDeliveryTarget channelOnly(AlertChannel channel) {
		return of(channel, null);
	}

	public static Set<AlertDeliveryTarget> copyOf(Set<AlertDeliveryTarget> targets) {
		if (targets == null || targets.isEmpty()) {
			throw new IllegalArgumentException("deliveryTargets must not be empty");
		}
		Set<AlertDeliveryTarget> copy = new HashSet<>();
		for (AlertDeliveryTarget target : targets) {
			if (target == null) {
				throw new IllegalArgumentException("deliveryTargets must not contain null");
			}
			copy.add(AlertDeliveryTarget.of(target.channel, target.chatRoomId));
		}
		return copy;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AlertDeliveryTarget target)) {
			return false;
		}
		return channel == target.channel
			&& Objects.equals(chatRoomId, target.chatRoomId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(channel, chatRoomId);
	}
}
