package com.vdt.log_monitoring.modules.alerting.internal.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vdt.log_monitoring.modules.alerting.internal.alert.AlertEntity;
import com.vdt.log_monitoring.modules.alerting.internal.alert.AlertService;
import com.vdt.log_monitoring.modules.alerting.internal.cache.AlertThresholdCache;
import com.vdt.log_monitoring.modules.alerting.internal.cache.AlertThresholdCache.DecisionType;
import com.vdt.log_monitoring.modules.alerting.internal.cache.AlertThresholdCache.ThresholdDecision;
import com.vdt.log_monitoring.modules.alerting.internal.notification.NotificationDispatcher;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertChannel;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertRuleDefinition;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertRuleEntity;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertRuleMatcher;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertRuleService;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertSeverity;

class AlertEvaluationServiceTest {

	private final AlertRuleService ruleService = mock();
	private final AlertRuleMatcher ruleMatcher = mock();
	private final AlertThresholdCache thresholdCache = mock();
	private final AlertService alertService = mock();
	private final NotificationDispatcher dispatcher = mock();
	private final AlertLogEvidenceReader logEvidenceReader = mock();
	private final AlertEvaluationLifecycle lifecycle = mock();
	private final AlertEvaluationService service = new AlertEvaluationService(
		ruleService, ruleMatcher, thresholdCache, alertService, logEvidenceReader, dispatcher, lifecycle);

	@BeforeEach
	void runAfterCommitCallbacksImmediately() {
		doAnswer(invocation -> {
			invocation.<Runnable>getArgument(0).run();
			return null;
		}).when(lifecycle).afterCommit(any());
	}

	@Test
	void triggerPersistsOccurrenceAndDispatchesAfterThreshold() {
		AlertRuleDefinition rule = rule();
		AlertEvaluationCandidate candidate = candidate();
		AlertEntity alert = mock();
		when(ruleService.findActiveRules(candidate.applicationId())).thenReturn(List.of(rule));
		when(ruleMatcher.matches(rule, AlertSeverity.ERROR, candidate.message(), candidate.logTimestamp())).thenReturn(true);
		when(thresholdCache.evaluate(rule, candidate.applicationId(), candidate.eventId(), candidate.logTimestamp()))
			.thenReturn(new ThresholdDecision(
				DecisionType.TRIGGERED, 3, Instant.parse("2026-06-18T03:59:00Z")));
		when(alertService.trigger(any(), any(), any(Long.class), any(), any()))
			.thenReturn(alert);

		assertThat(service.evaluate(candidate)).containsExactly(alert);

		verify(logEvidenceReader).findTopLogSamples(
			candidate.applicationId(),
			Instant.parse("2026-06-18T03:59:00Z"),
			candidate.logTimestamp(),
			rule.minSeverity());
		verify(dispatcher).dispatch(alert, rule);
	}

	@Test
	void belowThresholdDoesNotPersistOrDispatch() {
		AlertRuleDefinition rule = rule();
		AlertEvaluationCandidate candidate = candidate();
		when(ruleService.findActiveRules(candidate.applicationId())).thenReturn(List.of(rule));
		when(ruleMatcher.matches(rule, AlertSeverity.ERROR, candidate.message(), candidate.logTimestamp())).thenReturn(true);
		when(thresholdCache.evaluate(rule, candidate.applicationId(), candidate.eventId(), candidate.logTimestamp()))
			.thenReturn(new ThresholdDecision(
				DecisionType.BELOW_THRESHOLD, 2, Instant.parse("2026-06-18T03:59:00Z")));

		assertThat(service.evaluate(candidate)).isEmpty();

		verify(alertService, never()).trigger(any(), any(), any(Long.class), any(), any());
		verify(dispatcher, never()).dispatch(any(), any());
	}

	@Test
	void cooldownDoesNotInteractWithDbDirectly() {
		AlertRuleDefinition rule = rule();
		AlertEvaluationCandidate candidate = candidate();
		when(ruleService.findActiveRules(candidate.applicationId())).thenReturn(List.of(rule));
		when(ruleMatcher.matches(rule, AlertSeverity.ERROR, candidate.message(), candidate.logTimestamp())).thenReturn(true);
		when(thresholdCache.evaluate(rule, candidate.applicationId(), candidate.eventId(), candidate.logTimestamp()))
			.thenReturn(new ThresholdDecision(
				DecisionType.COOLDOWN, 4, Instant.parse("2026-06-18T03:59:00Z")));

		assertThat(service.evaluate(candidate)).isEmpty();

		verify(alertService, never()).recordOccurrenceOrTrigger(any(), any(), any(Long.class), any(), any());
		verify(dispatcher, never()).dispatch(any(), any());
	}

	private AlertRuleDefinition rule() {
		return AlertRuleDefinition.from(AlertRuleEntity.create(
			UUID.fromString("00000000-0000-0000-0000-000000000101"),
			"Payment failures", null, AlertSeverity.ERROR, AlertSeverity.CRITICAL, "payment", 3, 60, 120,
			null, null,
			AlertRuleEntity.channelOnlyTargets(Set.of(AlertChannel.WEBSOCKET)),
			UUID.fromString("00000000-0000-0000-0000-000000000102")));
	}

	private AlertEvaluationCandidate candidate() {
		return new AlertEvaluationCandidate(
			UUID.randomUUID(), UUID.randomUUID(),
			UUID.fromString("00000000-0000-0000-0000-000000000101"),
			"checkout-api", "Checkout API", "ERROR", "Payment failed",
			"checkout-payment", Instant.parse("2026-06-18T04:00:00Z"));
	}
}
