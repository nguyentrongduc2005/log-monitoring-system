package com.vdt.log_monitoring.modules.alerting.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.vdt.log_monitoring.modules.alerting.internal.alert.AlertService;
import com.vdt.log_monitoring.modules.alerting.internal.evaluation.AlertEvaluationService;
import com.vdt.log_monitoring.modules.alerting.internal.notification.ChatRoomService;
import com.vdt.log_monitoring.modules.alerting.internal.notification.telegram.TelegramChatDiscoveryService;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertChannel;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertRuleDefinition;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertRuleEntity;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertRuleMatcher;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertRuleService;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertSeverity;

class AlertingFacadeImplTest {

	private static final UUID APPLICATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");

	private final AlertRuleService alertRuleService = mock();
	private final AlertService alertService = mock();
	private final AlertEvaluationService alertEvaluationService = mock();
	private final ChatRoomService chatRoomService = mock();
	private final TelegramChatDiscoveryService telegramChatDiscoveryService = mock();
	private final AlertRuleMatcher alertRuleMatcher = mock();
	private final AlertingFacadeImpl facade = new AlertingFacadeImpl(
		alertRuleService,
		alertService,
		alertEvaluationService,
		chatRoomService,
		telegramChatDiscoveryService,
		alertRuleMatcher);

	@Test
	void matchingActiveRuleCandidatesUseSharedRuleServiceAndMatcher() {
		Instant timestamp = Instant.parse("2026-06-18T03:00:00Z");
		AlertRuleDefinition rule = rule();
		when(alertRuleService.findActiveRules(APPLICATION_ID)).thenReturn(List.of(rule));
		when(alertRuleMatcher.matches(rule, AlertSeverity.WARN, "inventory_reservation_mismatch", timestamp))
			.thenReturn(true);

		assertThat(facade.hasMatchingActiveRuleCandidate(
			APPLICATION_ID,
			"WARN",
			"inventory_reservation_mismatch",
			timestamp))
			.isTrue();
		verify(alertRuleService).findActiveRules(APPLICATION_ID);
	}

	private AlertRuleDefinition rule() {
		return AlertRuleDefinition.from(AlertRuleEntity.create(
			APPLICATION_ID,
			"Inventory reservation mismatch",
			null,
			AlertSeverity.WARN,
			AlertSeverity.WARN,
			"inventory_reservation_mismatch",
			2,
			300,
			300,
			null,
			null,
			AlertRuleEntity.channelOnlyTargets(Set.of(AlertChannel.WEBSOCKET)),
			UUID.fromString("00000000-0000-0000-0000-000000000102")));
	}
}
