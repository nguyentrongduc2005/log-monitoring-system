package com.vdt.log_monitoring.modules.alerting.internal;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.alerting.api.AlertingException;
import com.vdt.log_monitoring.modules.alerting.api.AlertingFacade;
import com.vdt.log_monitoring.modules.alerting.internal.alert.AlertEntity;
import com.vdt.log_monitoring.modules.alerting.internal.alert.AlertService;
import com.vdt.log_monitoring.modules.alerting.internal.evaluation.AlertEvaluationCandidate;
import com.vdt.log_monitoring.modules.alerting.internal.evaluation.AlertEvaluationService;
import com.vdt.log_monitoring.modules.alerting.internal.notification.ChatRoomEntity;
import com.vdt.log_monitoring.modules.alerting.internal.notification.ChatRoomService;
import com.vdt.log_monitoring.modules.alerting.internal.notification.telegram.TelegramChatDiscoveryService;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertChannel;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertDeliveryTarget;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertRuleEntity;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertRuleService;

@Component
@RequiredArgsConstructor
public class AlertingFacadeImpl implements AlertingFacade {

	private final AlertRuleService alertRuleService;
	private final AlertService alertService;
	private final AlertEvaluationService alertEvaluationService;
	private final ChatRoomService chatRoomService;
	private final TelegramChatDiscoveryService telegramChatDiscoveryService;

	@Override
	public AlertRuleDto createRule(CreateAlertRuleCommand command) {
		return mapRule(alertRuleService.createRule(
				command.applicationId(),
				command.name(),
				command.description(),
				command.minSeverity(),
				command.severity(),
				command.keywordPattern(),
				command.thresholdCount(),
				command.thresholdWindowSeconds(),
				command.cooldownSeconds(),
				command.activeStartTime(),
				command.activeEndTime(),
				command.channels(),
				command.deliveryTargets(),
				command.createdBy()));
	}

	@Override
	public AlertRuleDto updateRule(UUID ruleId, UpdateAlertRuleCommand command) {
		return mapRule(alertRuleService.updateRule(
				ruleId,
				command.name(),
				command.description(),
				command.minSeverity(),
				command.severity(),
				command.keywordPattern(),
				command.thresholdCount(),
				command.thresholdWindowSeconds(),
				command.cooldownSeconds(),
				command.activeStartTime(),
				command.activeEndTime(),
				command.channels(),
				command.deliveryTargets()));
	}

	@Override
	public AlertRuleDto changeRuleStatus(UUID ruleId, String status) {
		return mapRule(alertRuleService.changeStatus(ruleId, status));
	}

	@Override
	public void deleteRule(UUID ruleId) {
		alertRuleService.deleteRule(ruleId);
	}

	@Override
	public AlertRuleDto findRuleById(UUID ruleId) {
		return mapRule(alertRuleService.getRuleById(ruleId));
	}

	@Override
	public List<AlertRuleDto> findRules(UUID applicationId) {
		return alertRuleService.listRules(applicationId).stream()
				.sorted(Comparator.comparing(AlertRuleEntity::getCreatedAt).reversed())
				.map(this::mapRule)
				.toList();
	}

	@Override
	public List<ActiveAlertRuleCandidateDto> findActiveRuleCandidates(UUID applicationId) {
		return alertRuleService.findActiveRules(applicationId).stream()
				.map(rule -> new ActiveAlertRuleCandidateDto(
						rule.minSeverity().name(),
						rule.keywordPattern()))
				.toList();
	}

	@Override
	public List<AlertDto> evaluate(AlertingFacade.AlertCandidate candidate) {
		return alertEvaluationService.evaluate(
			new AlertEvaluationCandidate(
				candidate.eventId(),
				candidate.ingestionId(),
				candidate.applicationId(),
				candidate.applicationName(),
				candidate.applicationDisplayName(),
				candidate.severity(),
				candidate.message(),
				candidate.fingerprint(),
				candidate.logTimestamp())).stream()
				.map(this::mapAlert)
				.toList();
	}

	@Override
	public List<AlertDto> findAlerts(List<UUID> applicationIds, String status, String severity) {
		return alertService.listAlerts(applicationIds, status, severity).stream()
			.map(this::mapAlert)
			.toList();
	}

	@Override
	public List<AlertDto> findAlertsInWindow(UUID applicationId, java.time.Instant windowStart,
			java.time.Instant windowEnd) {
		return alertService.findAlertsInWindow(applicationId, windowStart, windowEnd).stream()
			.map(this::mapAlert)
			.toList();
	}

	@Override
	public AlertDto findAlertById(UUID alertId) {
		return mapAlert(alertService.getAlertById(alertId));
	}

	@Override
	public AlertDto acknowledgeAlert(UUID alertId, UUID acknowledgedBy) {
		return mapAlert(alertService.acknowledgeAlert(alertId, acknowledgedBy));
	}

	@Override
	public AlertDto resolveAlert(UUID alertId, UUID resolvedBy) {
		return mapAlert(alertService.resolveAlert(alertId, resolvedBy));
	}

	@Override
	public ChatRoomDto createChatRoom(CreateChatRoomCommand command) {
		return mapChatRoom(chatRoomService.createChatRoom(
				command.channel(),
				command.name(),
				command.chatId(),
				command.description(),
				command.createdBy()));
	}

	@Override
	public ChatRoomDto changeChatRoomStatus(UUID chatRoomId, String status) {
		return mapChatRoom(chatRoomService.changeStatus(chatRoomId, status));
	}

	@Override
	public List<ChatRoomDto> findChatRooms(String channel, String status) {
		return chatRoomService.listChatRooms(channel, status).stream()
				.map(this::mapChatRoom)
				.toList();
	}

	@Override
	public List<TelegramChatDto> discoverTelegramChats() {
		return telegramChatDiscoveryService.discoverChats().stream()
			.map(chat -> new TelegramChatDto(
				chat.chatId(),
				chat.name(),
				chat.type(),
				chat.username()))
			.toList();
	}

	private AlertRuleDto mapRule(AlertRuleEntity rule) {
		return new AlertRuleDto(
				rule.getId(),
				rule.getApplicationId(),
				rule.getName(),
				rule.getDescription(),
				rule.getMinSeverity().name(),
				rule.getSeverity().name(),
				rule.getKeywordPattern(),
				rule.getThresholdCount(),
				rule.getThresholdWindowSeconds(),
				rule.getCooldownSeconds(),
				formatTime(rule.getActiveStartTime()),
				formatTime(rule.getActiveEndTime()),
				rule.getStatus().name(),
				mapChannels(rule),
				mapDeliveryTargets(rule.getDeliveryTargets()),
				rule.getCreatedBy(),
				rule.getCreatedAt(),
				rule.getUpdatedAt());
	}

	private AlertDto mapAlert(AlertEntity alert) {
		return new AlertDto(
				alert.getId(),
				alert.getRuleId(),
				alert.getRuleName(),
				alert.getApplicationId(),
				alert.getApplicationName(),
				alert.getApplicationDisplayName(),
				alert.getSeverity().name(),
				alert.getTriggerType(),
				alert.getSourceType(),
				alert.getSourceId(),
				alert.getSummary(),
				alert.getMetadataJson(),
				alert.getLogSamples().stream().map(sample -> new com.vdt.log_monitoring.api.alerting.dto.AlertLogSampleDto(sample.level(), sample.message())).toList(),
				alert.getTriggeredAt(),
				alert.getOccurrenceCount(),
				alert.getFirstSeenAt(),
				alert.getLastSeenAt(),
				alert.getStatus().name(),
				mapDeliveryChannels(alert),
				mapDeliveryTargets(alert.getDeliveryTargets()),
				alert.getAcknowledgedBy(),
				alert.getAcknowledgedAt(),
				alert.getResolvedBy(),
				alert.getResolvedAt(),
				alert.getCreatedAt(),
				alert.getUpdatedAt());
	}

	private List<String> mapChannels(AlertRuleEntity rule) {
		return rule.getChannels().stream()
				.map(AlertChannel::name)
				.sorted()
				.toList();
	}

	private String formatTime(java.time.LocalTime time) {
		return time == null ? null : time.toString();
	}

	private List<String> mapDeliveryChannels(AlertEntity alert) {
		return alert.getDeliveryChannels().stream()
				.map(AlertChannel::name)
				.sorted()
				.toList();
	}

	private List<AlertDeliveryTargetDto> mapDeliveryTargets(Set<AlertDeliveryTarget> targets) {
		return targets.stream()
				.sorted(Comparator
						.comparing((AlertDeliveryTarget target) -> target.getChannel().name())
						.thenComparing(target -> String.valueOf(target.getChatRoomId())))
				.map(target -> new AlertDeliveryTargetDto(
						target.getChannel().name(),
						target.getChatRoomId()))
				.toList();
	}

	private ChatRoomDto mapChatRoom(ChatRoomEntity chatRoom) {
		return new ChatRoomDto(
				chatRoom.getId(),
				chatRoom.getChannel().name(),
				chatRoom.getName(),
				chatRoom.getChatId(),
				chatRoom.getDescription(),
				chatRoom.getStatus().name(),
				chatRoom.getCreatedBy(),
				chatRoom.getCreatedAt(),
				chatRoom.getUpdatedAt());
	}
}
