package com.vdt.log_monitoring.modules.alerting.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AlertingFacade {

	AlertRuleDto createRule(CreateAlertRuleCommand command);

	AlertRuleDto updateRule(UUID ruleId, UpdateAlertRuleCommand command);

	AlertRuleDto changeRuleStatus(UUID ruleId, String status);

	void deleteRule(UUID ruleId);

	AlertRuleDto findRuleById(UUID ruleId);

	List<AlertRuleDto> findRules(UUID applicationId);

	List<AlertDto> evaluate(AlertCandidate candidate);

	List<AlertDto> findAlerts(List<UUID> applicationIds, String status, String severity);

	List<AlertDto> findAlertsInWindow(UUID applicationId, Instant windowStart, Instant windowEnd);

	AlertDto findAlertById(UUID alertId);

	AlertDto acknowledgeAlert(UUID alertId, UUID acknowledgedBy);

	AlertDto resolveAlert(UUID alertId, UUID resolvedBy);

	ChatRoomDto createChatRoom(CreateChatRoomCommand command);

	ChatRoomDto changeChatRoomStatus(UUID chatRoomId, String status);

	List<ChatRoomDto> findChatRooms(String channel, String status);

	List<TelegramChatDto> discoverTelegramChats();

	record CreateAlertRuleCommand(
		UUID applicationId,
		String name,
		String description,
		String minSeverity,
		String severity,
		String keywordPattern,
		int thresholdCount,
		int thresholdWindowSeconds,
		int cooldownSeconds,
		List<String> channels,
		List<AlertDeliveryTargetCommand> deliveryTargets,
		UUID createdBy
	) {}

	record UpdateAlertRuleCommand(
		String name,
		String description,
		String minSeverity,
		String severity,
		String keywordPattern,
		int thresholdCount,
		int thresholdWindowSeconds,
		int cooldownSeconds,
		List<String> channels,
		List<AlertDeliveryTargetCommand> deliveryTargets
	) {}

	record AlertDeliveryTargetCommand(
		String channel,
		UUID chatRoomId
	) {}

	record CreateChatRoomCommand(
		String channel,
		String name,
		String chatId,
		String description,
		UUID createdBy
	) {}

	record AlertCandidate(
		UUID eventId,
		UUID ingestionId,
		UUID applicationId,
		String applicationName,
		String applicationDisplayName,
		String severity,
		String message,
		String fingerprint,
		Instant logTimestamp
	) {}

	record AlertRuleDto(
		UUID id,
		UUID applicationId,
		String name,
		String description,
		String minSeverity,
		String severity,
		String keywordPattern,
		int thresholdCount,
		int thresholdWindowSeconds,
		int cooldownSeconds,
		String status,
		List<String> channels,
		List<AlertDeliveryTargetDto> deliveryTargets,
		UUID createdBy,
		Instant createdAt,
		Instant updatedAt
	) {}

	record AlertDeliveryTargetDto(
		String channel,
		UUID chatRoomId
	) {}

	record ChatRoomDto(
		UUID id,
		String channel,
		String name,
		String chatId,
		String description,
		String status,
		UUID createdBy,
		Instant createdAt,
		Instant updatedAt
	) {}

	record TelegramChatDto(
		String chatId,
		String name,
		String type,
		String username
	) {}

	record AlertDto(
		UUID id,
		UUID ruleId,
		String ruleName,
		UUID applicationId,
		String applicationName,
		String applicationDisplayName,
		String severity,
		String triggerType,
		String sourceType,
		UUID sourceId,
		String summary,
		String metadataJson,
		List<com.vdt.log_monitoring.api.alerting.dto.AlertLogSampleDto> logSamples,
		Instant triggeredAt,
		long occurrenceCount,
		Instant firstSeenAt,
		Instant lastSeenAt,
		String status,
		List<String> dispatchedChannels,
		List<AlertDeliveryTargetDto> deliveryTargets,
		UUID acknowledgedBy,
		Instant acknowledgedAt,
		UUID resolvedBy,
		Instant resolvedAt,
		Instant createdAt,
		Instant updatedAt
	) {}
}
