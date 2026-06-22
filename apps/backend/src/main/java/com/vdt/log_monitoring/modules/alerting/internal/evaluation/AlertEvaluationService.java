package com.vdt.log_monitoring.modules.alerting.internal.evaluation;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vdt.log_monitoring.modules.alerting.api.AlertingException;
import com.vdt.log_monitoring.modules.alerting.internal.alert.AlertEntity;
import com.vdt.log_monitoring.modules.alerting.internal.alert.AlertService;
import com.vdt.log_monitoring.modules.alerting.internal.cache.AlertThresholdCache;
import com.vdt.log_monitoring.modules.alerting.internal.cache.AlertThresholdCache.DecisionType;
import com.vdt.log_monitoring.modules.alerting.internal.cache.AlertThresholdCache.ThresholdDecision;
import com.vdt.log_monitoring.modules.alerting.internal.notification.NotificationDispatcher;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertRuleDefinition;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertRuleMatcher;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertRuleService;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertSeverity;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertEvaluationService {

	private final AlertRuleService ruleService;
	private final AlertRuleMatcher ruleMatcher;
	private final AlertThresholdCache thresholdCache;
	private final AlertService alertService;
	private final NotificationDispatcher notificationDispatcher;
	private final AlertEvaluationLifecycle lifecycle;

	@Transactional
	public List<AlertEntity> evaluate(AlertEvaluationCandidate candidate) {
		AlertSeverity severity = parseSeverity(candidate.severity());
		return ruleService.findActiveRules(candidate.applicationId()).stream()
			.filter(rule -> ruleMatcher.matches(rule, severity, candidate.message()))
			.map(rule -> evaluate(rule, candidate, severity))
			.flatMap(List::stream)
			.toList();
	}

	private List<AlertEntity> evaluate(
		AlertRuleDefinition rule,
		AlertEvaluationCandidate candidate,
		AlertSeverity severity
	) {
		ThresholdDecision decision = thresholdCache.evaluate(
			rule, candidate.eventId(), candidate.fingerprint(), candidate.logTimestamp());
		registerRollback(rule, candidate, decision);

		return switch (decision.type()) {
			case TRIGGERED -> List.of(trigger(rule, candidate, severity, decision));
			case COOLDOWN -> {
				alertService.recordOccurrence(rule.id(), candidate.fingerprint(), candidate.logTimestamp());
				yield List.of();
			}
			case DUPLICATE, BELOW_THRESHOLD -> List.of();
		};
	}

	private AlertEntity trigger(
		AlertRuleDefinition rule,
		AlertEvaluationCandidate candidate,
		AlertSeverity severity,
		ThresholdDecision decision
	) {
		AlertEntity alert = alertService.trigger(
			rule,
			candidate.toOccurrenceData(),
			severity,
			decision.count(),
			decision.firstSeenAt());
		lifecycle.afterCommit(() -> notificationDispatcher.dispatch(alert, rule));
		return alert;
	}

	private void registerRollback(
		AlertRuleDefinition rule,
		AlertEvaluationCandidate candidate,
		ThresholdDecision decision
	) {
		if (decision.type() == DecisionType.DUPLICATE) {
			return;
		}
		lifecycle.afterRollback(() -> {
			try {
				thresholdCache.rollback(rule, candidate.eventId(), candidate.fingerprint(), decision);
			} catch (RuntimeException exception) {
				log.error(
					"Failed to roll back Redis alert evaluation ruleId={} eventId={}",
					rule.id(), candidate.eventId(), exception);
			}
		});
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
}
