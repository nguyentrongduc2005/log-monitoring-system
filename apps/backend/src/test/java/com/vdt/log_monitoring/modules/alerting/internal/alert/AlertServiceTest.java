package com.vdt.log_monitoring.modules.alerting.internal.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.vdt.log_monitoring.modules.alerting.internal.notification.NotificationDispatcher;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertChannel;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertRuleEntity;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertRuleService;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertSeverity;

class AlertServiceTest {

	private static final UUID APPLICATION_ID =
		UUID.fromString("00000000-0000-0000-0000-000000000101");
	private static final UUID CREATED_BY =
		UUID.fromString("00000000-0000-0000-0000-000000000102");

	private final AlertRepository alertRepository = org.mockito.Mockito.mock();
	private final AlertRuleService alertRuleService = org.mockito.Mockito.mock();
	private final NotificationDispatcher notificationDispatcher = org.mockito.Mockito.mock();
	private final AlertService alertService = new AlertService(
		alertRepository,
		alertRuleService,
		notificationDispatcher
	);

	@Test
	void evaluateCreatesAlertAndDispatchesWhenRuleMatches() {
		AlertRuleEntity rule = rule("Payment failures", AlertSeverity.ERROR, "payment");
		when(alertRuleService.findActiveRules(APPLICATION_ID)).thenReturn(List.of(rule));
		when(alertRepository.save(any(AlertEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		List<AlertService.AlertDispatch> dispatches = alertService.evaluate(candidate(
			"ERROR",
			"Payment failed after checkout"
		));

		assertThat(dispatches).hasSize(1);
		AlertEntity alert = dispatches.getFirst().alert();
		assertThat(alert.getRuleId()).isEqualTo(rule.getId());
		assertThat(alert.getApplicationId()).isEqualTo(APPLICATION_ID);
		assertThat(alert.getSeverity()).isEqualTo(AlertSeverity.ERROR);
		assertThat(alert.getMessage()).isEqualTo("Payment failed after checkout");
		assertThat(alert.getDeliveryChannels()).containsExactlyInAnyOrder(
			AlertChannel.TELEGRAM,
			AlertChannel.WEBSOCKET
		);

		ArgumentCaptor<AlertEntity> alertCaptor = ArgumentCaptor.forClass(AlertEntity.class);
		verify(alertRepository).save(alertCaptor.capture());
		verify(notificationDispatcher).dispatch(alertCaptor.getValue(), rule);
	}

	@Test
	void evaluateSkipsRuleWhenKeywordDoesNotMatch() {
		AlertRuleEntity rule = rule("Payment failures", AlertSeverity.ERROR, "payment");
		when(alertRuleService.findActiveRules(APPLICATION_ID)).thenReturn(List.of(rule));

		List<AlertService.AlertDispatch> dispatches = alertService.evaluate(candidate(
			"CRITICAL",
			"Database connection failed"
		));

		assertThat(dispatches).isEmpty();
		verify(alertRepository, never()).save(any());
		verify(notificationDispatcher, never()).dispatch(any(), any());
	}

	private AlertRuleEntity rule(String name, AlertSeverity severity, String keyword) {
		return AlertRuleEntity.create(
			APPLICATION_ID,
			name,
			null,
			severity,
			keyword,
			1,
			60,
			60,
			AlertRuleEntity.channelOnlyTargets(Set.of(AlertChannel.TELEGRAM, AlertChannel.WEBSOCKET)),
			CREATED_BY
		);
	}

	private AlertService.AlertCandidateData candidate(String severity, String message) {
		return new AlertService.AlertCandidateData(
			UUID.fromString("00000000-0000-0000-0000-000000000201"),
			UUID.fromString("00000000-0000-0000-0000-000000000202"),
			APPLICATION_ID,
			"checkout-api",
			"Checkout API",
			severity,
			message,
			"checkout-payment",
			Instant.parse("2026-06-18T04:00:00Z")
		);
	}
}
