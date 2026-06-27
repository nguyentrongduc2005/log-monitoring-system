package com.vdt.log_monitoring.modules.alerting.internal.rule;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record AlertRuleDefinition(
	UUID id,
	UUID applicationId,
	String name,
	AlertSeverity minSeverity,
	AlertSeverity severity,
	String keywordPattern,
	int thresholdCount,
	int thresholdWindowSeconds,
	int cooldownSeconds,
	Set<DeliveryTarget> deliveryTargets
) {

	public static AlertRuleDefinition from(AlertRuleEntity rule) {
		return new AlertRuleDefinition(
			rule.getId(),
			rule.getApplicationId(),
			rule.getName(),
			rule.getMinSeverity(),
			rule.getSeverity(),
			rule.getKeywordPattern(),
			rule.getThresholdCount(),
			rule.getThresholdWindowSeconds(),
			rule.getCooldownSeconds(),
			rule.getDeliveryTargets().stream()
				.map(DeliveryTarget::from)
				.collect(Collectors.toUnmodifiableSet()));
	}

	public Set<AlertDeliveryTarget> toDeliveryTargets() {
		return deliveryTargets.stream()
			.map(DeliveryTarget::toEntityValue)
			.collect(Collectors.toUnmodifiableSet());
	}

	public record DeliveryTarget(AlertChannel channel, UUID chatRoomId) {

		private static DeliveryTarget from(AlertDeliveryTarget target) {
			return new DeliveryTarget(target.getChannel(), target.getChatRoomId());
		}

		private AlertDeliveryTarget toEntityValue() {
			return AlertDeliveryTarget.of(channel, chatRoomId);
		}
	}
}
