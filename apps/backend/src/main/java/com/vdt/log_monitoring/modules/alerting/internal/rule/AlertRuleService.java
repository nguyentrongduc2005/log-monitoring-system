package com.vdt.log_monitoring.modules.alerting.internal.rule;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vdt.log_monitoring.modules.alerting.api.AlertingException;
import com.vdt.log_monitoring.modules.alerting.api.AlertingFacade.AlertDeliveryTargetCommand;
import com.vdt.log_monitoring.modules.alerting.internal.notification.ChatRoomEntity;
import com.vdt.log_monitoring.modules.alerting.internal.notification.ChatRoomRepository;
import com.vdt.log_monitoring.modules.alerting.internal.notification.ChatRoomStatus;
import com.vdt.log_monitoring.modules.identity.api.ApplicationAccessFacade;

@Service
@RequiredArgsConstructor
public class AlertRuleService {

	private final AlertRuleRepository alertRuleRepository;
	private final ChatRoomRepository chatRoomRepository;
	private final ApplicationAccessFacade applicationAccessFacade;

	@Transactional
	public AlertRuleEntity createRule(
		UUID applicationId,
		String name,
		String description,
		String minSeverity,
		String keywordPattern,
		int thresholdCount,
		int thresholdWindowSeconds,
		int cooldownSeconds,
		List<String> channels,
		List<AlertDeliveryTargetCommand> deliveryTargets,
		UUID createdBy
	) {
		applicationAccessFacade.findApplicationById(applicationId);
		String normalizedName = requireText(name, "name");
		if (alertRuleRepository.existsByApplicationIdAndNameIgnoreCase(applicationId, normalizedName)) {
			throw new AlertingException(
				AlertingException.ErrorCode.ALERT_RULE_NAME_ALREADY_EXISTS,
				"Alert rule name is already registered for this application"
			);
		}

		return alertRuleRepository.save(AlertRuleEntity.create(
			applicationId,
			normalizedName,
			description,
			parseSeverity(minSeverity),
			keywordPattern,
			thresholdCount,
			thresholdWindowSeconds,
			cooldownSeconds,
			parseDeliveryTargets(channels, deliveryTargets),
			createdBy
		));
	}

	@Transactional
	public AlertRuleEntity updateRule(
		UUID ruleId,
		String name,
		String description,
		String minSeverity,
		String keywordPattern,
		int thresholdCount,
		int thresholdWindowSeconds,
		int cooldownSeconds,
		List<String> channels,
		List<AlertDeliveryTargetCommand> deliveryTargets
	) {
		AlertRuleEntity rule = getRuleById(ruleId);
		String normalizedName = requireText(name, "name");
		if (!rule.getName().equalsIgnoreCase(normalizedName)
			&& alertRuleRepository.existsByApplicationIdAndNameIgnoreCase(
				rule.getApplicationId(),
				normalizedName
			)) {
			throw new AlertingException(
				AlertingException.ErrorCode.ALERT_RULE_NAME_ALREADY_EXISTS,
				"Alert rule name is already registered for this application"
			);
		}

		rule.updateRule(
			normalizedName,
			description,
			parseSeverity(minSeverity),
			keywordPattern,
			thresholdCount,
			thresholdWindowSeconds,
			cooldownSeconds,
			parseDeliveryTargets(channels, deliveryTargets)
		);
		return rule;
	}

	@Transactional
	public AlertRuleEntity changeStatus(UUID ruleId, String status) {
		AlertRuleEntity rule = getRuleById(ruleId);
		rule.changeStatus(parseStatus(status));
		return rule;
	}

	@Transactional
	public void deleteRule(UUID ruleId) {
		AlertRuleEntity rule = getRuleById(ruleId);
		alertRuleRepository.delete(rule);
	}

	@Transactional(readOnly = true)
	public AlertRuleEntity getRuleById(UUID ruleId) {
		return alertRuleRepository.findById(ruleId)
			.orElseThrow(() -> new AlertingException(
				AlertingException.ErrorCode.ALERT_RULE_NOT_FOUND,
				"Alert rule not found"
			));
	}

	@Transactional(readOnly = true)
	public List<AlertRuleEntity> listRules(UUID applicationId) {
		if (applicationId == null) {
			return alertRuleRepository.findAll();
		}
		return alertRuleRepository.findByApplicationId(applicationId);
	}

	@Transactional(readOnly = true)
	public List<AlertRuleEntity> findActiveRules(UUID applicationId) {
		return alertRuleRepository.findByApplicationIdAndStatus(
			applicationId,
			AlertRuleStatus.ACTIVE
		);
	}

	private AlertRuleStatus parseStatus(String status) {
		try {
			return AlertRuleStatus.valueOf(requireText(status, "status").toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new AlertingException(
				AlertingException.ErrorCode.INVALID_ALERT_STATUS,
				"Invalid alert rule status"
			);
		}
	}

	private AlertSeverity parseSeverity(String severity) {
		try {
			return AlertSeverity.from(severity);
		} catch (IllegalArgumentException exception) {
			throw new AlertingException(
				AlertingException.ErrorCode.INVALID_ALERT_SEVERITY,
				"Invalid alert severity"
			);
		}
	}

	private Set<AlertChannel> parseChannels(List<String> channels) {
		if (channels == null || channels.isEmpty()) {
			throw new AlertingException(
				AlertingException.ErrorCode.INVALID_ALERT_CHANNEL,
				"At least one alert channel is required"
			);
		}

		try {
			return channels.stream()
				.map(this::parseChannel)
				.collect(Collectors.toUnmodifiableSet());
		} catch (IllegalArgumentException exception) {
			throw new AlertingException(
				AlertingException.ErrorCode.INVALID_ALERT_CHANNEL,
				"Invalid alert channel"
			);
		}
	}

	private Set<AlertDeliveryTarget> parseDeliveryTargets(
		List<String> channels,
		List<AlertDeliveryTargetCommand> deliveryTargets
	) {
		if (deliveryTargets == null || deliveryTargets.isEmpty()) {
			return AlertRuleEntity.channelOnlyTargets(parseChannels(channels));
		}

		try {
			Set<AlertDeliveryTarget> parsedTargets = deliveryTargets.stream()
				.map(this::parseDeliveryTarget)
				.collect(Collectors.toUnmodifiableSet());
			if (parsedTargets.size() != deliveryTargets.size()) {
				throw new IllegalArgumentException("duplicate delivery target");
			}
			long channelCount = parsedTargets.stream()
				.map(AlertDeliveryTarget::getChannel)
				.distinct()
				.count();
			if (channelCount != parsedTargets.size()) {
				throw new IllegalArgumentException("duplicate delivery channel");
			}
			return parsedTargets;
		} catch (IllegalArgumentException exception) {
			throw new AlertingException(
				AlertingException.ErrorCode.INVALID_CHAT_ROOM,
				"Invalid alert delivery target"
			);
		}
	}

	private AlertDeliveryTarget parseDeliveryTarget(AlertDeliveryTargetCommand target) {
		if (target == null) {
			throw new IllegalArgumentException("delivery target must not be null");
		}
		AlertChannel channel = parseChannel(target.channel());
		UUID chatRoomId = Objects.requireNonNull(target.chatRoomId(), "chatRoomId must not be null");
		ChatRoomEntity chatRoom = chatRoomRepository.findById(chatRoomId)
			.orElseThrow(() -> new AlertingException(
				AlertingException.ErrorCode.CHAT_ROOM_NOT_FOUND,
				"Chat room not found"
			));
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

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new AlertingException(
				AlertingException.ErrorCode.INVALID_ALERT_RULE,
				fieldName + " must not be blank"
			);
		}
		return value.trim();
	}
}
