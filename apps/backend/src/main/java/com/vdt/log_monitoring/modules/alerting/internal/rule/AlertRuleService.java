package com.vdt.log_monitoring.modules.alerting.internal.rule;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vdt.log_monitoring.modules.alerting.api.AlertingException;
import com.vdt.log_monitoring.modules.alerting.api.AlertingFacade.AlertDeliveryTargetCommand;
import com.vdt.log_monitoring.modules.alerting.internal.cache.AlertRuleCache;
import com.vdt.log_monitoring.modules.identity.api.ApplicationAccessFacade;

@Service
@RequiredArgsConstructor
public class AlertRuleService {

	private final AlertRuleRepository alertRuleRepository;
	private final ApplicationAccessFacade applicationAccessFacade;
	private final AlertRuleCache alertRuleCache;
	private final AlertDeliveryTargetResolver deliveryTargetResolver;

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

		AlertRuleEntity rule = alertRuleRepository.save(AlertRuleEntity.create(
			applicationId,
			normalizedName,
			description,
			parseSeverity(minSeverity),
			keywordPattern,
			thresholdCount,
			thresholdWindowSeconds,
			cooldownSeconds,
			deliveryTargetResolver.resolve(channels, deliveryTargets),
			createdBy
		));
		alertRuleCache.evictAfterCommit(applicationId);
		return rule;
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
			deliveryTargetResolver.resolve(channels, deliveryTargets)
		);
		alertRuleCache.evictAfterCommit(rule.getApplicationId());
		return rule;
	}

	@Transactional
	public AlertRuleEntity changeStatus(UUID ruleId, String status) {
		AlertRuleEntity rule = getRuleById(ruleId);
		rule.changeStatus(parseStatus(status));
		alertRuleCache.evictAfterCommit(rule.getApplicationId());
		return rule;
	}

	@Transactional
	public void deleteRule(UUID ruleId) {
		AlertRuleEntity rule = getRuleById(ruleId);
		alertRuleRepository.delete(rule);
		alertRuleCache.evictAfterCommit(rule.getApplicationId());
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
	public List<AlertRuleDefinition> findActiveRules(UUID applicationId) {
		return alertRuleCache.getActiveRules(applicationId, () ->
			alertRuleRepository.findByApplicationIdAndStatus(applicationId, AlertRuleStatus.ACTIVE)
				.stream()
				.map(AlertRuleDefinition::from)
				.toList());
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
