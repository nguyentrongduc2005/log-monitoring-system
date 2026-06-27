package com.vdt.log_monitoring.modules.alerting.internal.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertChannel;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertDeliveryTarget;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertRuleDefinition;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertRuleEntity;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertSeverity;

class AlertServiceTest {

	private final AlertRepository repository = mock();
	private final AlertService service = new AlertService(repository);

	@Test
	void triggerCreatesOccurrenceWhenNoActiveAlertExists() {
		AlertRuleDefinition rule = rule();
		when(repository.findFirstByRuleIdAndApplicationIdAndStatusNotOrderByTriggeredAtDesc(
			rule.id(), rule.applicationId(), AlertStatus.RESOLVED)).thenReturn(Optional.empty());
		when(repository.save(any(AlertEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		AlertEntity alert = service.trigger(
			rule, occurrence(), 3, Instant.parse("2026-06-18T03:59:00Z"), List.<AlertLogSample>of());

		assertThat(alert.getOccurrenceCount()).isEqualTo(3);
		assertThat(alert.getFirstSeenAt()).isEqualTo(Instant.parse("2026-06-18T03:59:00Z"));
		assertThat(alert.getDeliveryTargets()).hasSize(1);
		verify(repository).save(alert);
	}

	@Test
	void triggerReusesExistingUnresolvedOccurrence() {
		AlertRuleDefinition rule = rule();
		AlertEntity existing = existingAlert(rule);
		when(repository.findFirstByRuleIdAndApplicationIdAndStatusNotOrderByTriggeredAtDesc(
			rule.id(), rule.applicationId(), AlertStatus.RESOLVED)).thenReturn(Optional.of(existing));

		AlertEntity result = service.trigger(
			rule, occurrence(), 3, Instant.parse("2026-06-18T03:59:00Z"), List.<AlertLogSample>of());

		assertThat(result).isSameAs(existing);
		assertThat(result.getOccurrenceCount()).isEqualTo(2);
		verify(repository, never()).save(any());
	}

	@Test
	void recordOccurrenceUpdatesActiveAlertDuringCooldown() {
		AlertRuleDefinition rule = rule();
		AlertEntity existing = existingAlert(rule);
		when(repository.findFirstByRuleIdAndApplicationIdAndStatusNotOrderByTriggeredAtDesc(
			rule.id(), rule.applicationId(), AlertStatus.RESOLVED)).thenReturn(Optional.of(existing));

		service.recordOccurrence(
			rule.id(), rule.applicationId(), Instant.parse("2026-06-18T04:00:00Z"));

		assertThat(existing.getOccurrenceCount()).isEqualTo(2);
		assertThat(existing.getLastSeenAt()).isEqualTo(Instant.parse("2026-06-18T04:00:00Z"));
	}

	@Test
	void cooldownRecordDoesNotCreateAlertWhenActiveAlertExists() {
		AlertRuleDefinition rule = rule();
		AlertEntity existing = existingAlert(rule);
		when(repository.findFirstByRuleIdAndApplicationIdAndStatusNotOrderByTriggeredAtDesc(
			rule.id(), rule.applicationId(), AlertStatus.RESOLVED)).thenReturn(Optional.of(existing));

		Optional<AlertEntity> result = service.recordOccurrenceOrTrigger(
			rule, occurrence(), 4, Instant.parse("2026-06-18T03:59:00Z"), List.<AlertLogSample>of());

		assertThat(result).isEmpty();
		assertThat(existing.getOccurrenceCount()).isEqualTo(2);
		verify(repository, never()).save(any());
	}

	@Test
	void cooldownRecordCreatesAlertWhenPreviousOccurrenceWasResolved() {
		AlertRuleDefinition rule = rule();
		when(repository.findFirstByRuleIdAndApplicationIdAndStatusNotOrderByTriggeredAtDesc(
			rule.id(), rule.applicationId(), AlertStatus.RESOLVED)).thenReturn(Optional.empty());
		when(repository.save(any(AlertEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Optional<AlertEntity> result = service.recordOccurrenceOrTrigger(
			rule, occurrence(), 4, Instant.parse("2026-06-18T03:59:00Z"), List.<AlertLogSample>of());

		assertThat(result).isPresent();
		assertThat(result.orElseThrow().getOccurrenceCount()).isEqualTo(4);
		assertThat(result.orElseThrow().getStatus()).isEqualTo(AlertStatus.OPEN);
		verify(repository).save(result.orElseThrow());
	}

	@Test
	void listAlertsFiltersVisibleApplicationsByStatusAndSeverity() {
		AlertRuleDefinition rule = rule();
		AlertEntity openError = existingAlert(rule);
		AlertEntity resolvedError = existingAlert(rule);
		resolvedError.resolve(UUID.randomUUID());
		when(repository.findByApplicationIdInOrderByTriggeredAtDesc(List.of(rule.applicationId())))
			.thenReturn(List.of(openError, resolvedError));

		List<AlertEntity> result = service.listAlerts(
			List.of(rule.applicationId()), "OPEN", "ERROR");

		assertThat(result).containsExactly(openError);
	}

	private AlertRuleDefinition rule() {
		return AlertRuleDefinition.from(AlertRuleEntity.create(
			UUID.fromString("00000000-0000-0000-0000-000000000101"),
			"Payment failures", null, AlertSeverity.ERROR, AlertSeverity.CRITICAL, "payment", 3, 60, 120,
			AlertRuleEntity.channelOnlyTargets(Set.of(AlertChannel.WEBSOCKET)),
			UUID.fromString("00000000-0000-0000-0000-000000000102")));
	}

	private AlertOccurrenceData occurrence() {
		return new AlertOccurrenceData(
			UUID.randomUUID(), UUID.randomUUID(), rule().applicationId(),
			"checkout-api", "Checkout API", "Payment failed", "checkout-payment",
			Instant.parse("2026-06-18T04:00:00Z"));
	}

	private AlertEntity existingAlert(AlertRuleDefinition rule) {
		return AlertEntity.create(
			rule.id(), rule.applicationId(),
			"checkout-api", "Checkout API", rule.name(), AlertSeverity.ERROR, List.of(new AlertLogSample("ERROR", "Payment failed")),
			Instant.parse("2026-06-18T03:59:00Z"), Instant.parse("2026-06-18T03:59:00Z"), Instant.parse("2026-06-18T03:59:00Z"),
			Set.of(AlertDeliveryTarget.channelOnly(AlertChannel.WEBSOCKET)), 1);
	}
}
