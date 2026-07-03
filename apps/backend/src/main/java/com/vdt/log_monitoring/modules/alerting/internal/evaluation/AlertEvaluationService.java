package com.vdt.log_monitoring.modules.alerting.internal.evaluation;

import java.util.List;

import com.vdt.log_monitoring.modules.alerting.internal.alert.AlertLogSample;
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
	private final AlertLogEvidenceReader evidenceReader;
	private final NotificationDispatcher notificationDispatcher;
	private final AlertEvaluationLifecycle lifecycle;

	@Transactional
	public List<AlertEntity> evaluate(AlertEvaluationCandidate candidate) {
		AlertSeverity severity = parseSeverity(candidate.severity());
		return ruleService.findActiveRules(candidate.applicationId()).stream()
			.filter(rule -> ruleMatcher.matches(rule, severity, candidate.message(), candidate.logTimestamp()))
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
			rule, candidate.applicationId(), candidate.eventId(), candidate.logTimestamp());
		registerRollback(rule, candidate, decision);

		return switch (decision.type()) {
			case TRIGGERED -> {
				List<AlertLogSample> logSamples = evidenceReader.findTopLogSamples(
					candidate.applicationId(),
					decision.firstSeenAt(),
					candidate.logTimestamp(),
					rule.minSeverity());
				yield List.of(trigger(rule, candidate, decision, logSamples));
			}
			case COOLDOWN -> {
				// Cập nhật số lượng occurrence_count và last_seen_at sẽ được đồng bộ ngầm qua AlertSyncScheduler
				yield List.of();
			}
			case DUPLICATE, BELOW_THRESHOLD -> List.of();
		};
	}

	private AlertEntity trigger(
		AlertRuleDefinition rule,
		AlertEvaluationCandidate candidate,
		ThresholdDecision decision,
		List<AlertLogSample> logSamples
	) {
		AlertEntity alert = alertService.trigger(
			rule,
			candidate.toOccurrenceData(),
			decision.count(),
			decision.firstSeenAt(),
			logSamples);
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
				thresholdCache.rollback(rule, candidate.applicationId(), candidate.eventId(), decision);
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
