package com.vdt.log_monitoring.modules.alerting.internal.alert;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vdt.log_monitoring.modules.alerting.api.AlertingException;
import com.vdt.log_monitoring.modules.alerting.internal.notification.NotificationDispatcher;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertRuleEntity;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertRuleService;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertRuleStatus;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertSeverity;

@Service
@RequiredArgsConstructor
public class AlertService {

	private final AlertRepository alertRepository;
	private final AlertRuleService alertRuleService;
	private final NotificationDispatcher notificationDispatcher;

	@Transactional
	public List<AlertDispatch> evaluate(AlertCandidateData candidate) {
		AlertSeverity candidateSeverity = parseSeverity(candidate.severity());
		return alertRuleService.findActiveRules(candidate.applicationId()).stream()
				.filter(rule -> matches(rule, candidateSeverity, candidate.message()))
				.map(rule -> createAndDispatchAlert(rule, candidate, candidateSeverity))
				.toList();
	}

	@Transactional
	public AlertEntity acknowledgeAlert(UUID alertId, UUID acknowledgedBy) {
		AlertEntity alert = getAlertById(alertId);
		alert.acknowledge(acknowledgedBy);
		return alert;
	}

	@Transactional
	public AlertEntity resolveAlert(UUID alertId, UUID resolvedBy) {
		AlertEntity alert = getAlertById(alertId);
		alert.resolve(resolvedBy);
		return alert;
	}

	@Transactional(readOnly = true)
	public AlertEntity getAlertById(UUID alertId) {
		return alertRepository.findById(alertId)
				.orElseThrow(() -> new AlertingException(
						AlertingException.ErrorCode.ALERT_NOT_FOUND,
						"Alert not found"));
	}

	private AlertDispatch createAndDispatchAlert(
			AlertRuleEntity rule,
			AlertCandidateData candidate,
			AlertSeverity severity) {
		AlertEntity alert = alertRepository.save(AlertEntity.create(
				rule.getId(),
				candidate.applicationId(),
				candidate.eventId(),
				candidate.ingestionId(),
				candidate.applicationName(),
				candidate.applicationDisplayName(),
				severity,
				candidate.message(),
				candidate.fingerprint(),
				candidate.logTimestamp(),
				rule.getDeliveryTargets()));
		notificationDispatcher.dispatch(alert, rule);
		return new AlertDispatch(alert, rule);
	}

	private boolean matches(AlertRuleEntity rule, AlertSeverity severity, String message) {
		return rule.getStatus() == AlertRuleStatus.ACTIVE
				&& severity.ordinal() >= rule.getMinSeverity().ordinal()
				&& matchesKeyword(rule, message);
	}

	private boolean matchesKeyword(AlertRuleEntity rule, String message) {
		String keywordPattern = rule.getKeywordPattern();
		if (keywordPattern == null || keywordPattern.isBlank()) {
			return true;
		}
		return message != null
				&& message.toLowerCase().contains(keywordPattern.toLowerCase());
	}

	private AlertSeverity parseSeverity(String severity) {
		try {
			return AlertSeverity.from(severity);
		} catch (IllegalArgumentException exception) {
			throw new AlertingException(
					AlertingException.ErrorCode.INVALID_ALERT_SEVERITY,
					"Invalid alert severity");
		}
	}

	public record AlertCandidateData(
			UUID eventId,
			UUID ingestionId,
			UUID applicationId,
			String applicationName,
			String applicationDisplayName,
			String severity,
			String message,
			String fingerprint,
			Instant logTimestamp) {
	}

	public record AlertDispatch(
			AlertEntity alert,
			AlertRuleEntity rule) {
	}
}
