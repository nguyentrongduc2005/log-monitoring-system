package com.vdt.log_monitoring.modules.alerting.internal.alert;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertChannel;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertDeliveryTarget;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertSeverity;

class AlertEntityTest {

	@Test
	void createStoresAlertSnapshotAndDeliveryChannels() {
		Instant logTimestamp = Instant.parse("2026-06-18T03:00:00Z");

		AlertEntity alert = AlertEntity.create(
			UUID.fromString("00000000-0000-0000-0000-000000000001"),
			UUID.fromString("00000000-0000-0000-0000-000000000002"),
			" checkout-api ",
			" Checkout API ",
			"Payment failures",
			AlertSeverity.CRITICAL,
			java.util.List.of(new AlertLogSample("ERROR", "Payment failure spike")),
			logTimestamp,
			logTimestamp,
			logTimestamp,
			Set.of(
				AlertDeliveryTarget.channelOnly(AlertChannel.TELEGRAM),
				AlertDeliveryTarget.channelOnly(AlertChannel.WEBSOCKET)
			),
			1
		);

		assertThat(alert.getId()).isNotNull();
		assertThat(alert.getApplicationName()).isEqualTo("checkout-api");
		assertThat(alert.getApplicationDisplayName()).isEqualTo("Checkout API");
		assertThat(alert.getRuleName()).isEqualTo("Payment failures");
		assertThat(alert.getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
		assertThat(alert.getLogSamples()).containsExactly(new AlertLogSample("ERROR", "Payment failure spike"));
		assertThat(alert.getTriggeredAt()).isNotNull();
		assertThat(alert.getOccurrenceCount()).isEqualTo(1);
		assertThat(alert.getFirstSeenAt()).isEqualTo(logTimestamp);
		assertThat(alert.getLastSeenAt()).isEqualTo(logTimestamp);
		assertThat(alert.getStatus()).isEqualTo(AlertStatus.OPEN);
		assertThat(alert.getDeliveryChannels()).containsExactlyInAnyOrder(
			AlertChannel.TELEGRAM,
			AlertChannel.WEBSOCKET
		);
	}

	@Test
	void acknowledgeAndResolveAlert() {
		Instant ts = Instant.parse("2026-06-18T03:00:00Z");
		AlertEntity alert = AlertEntity.create(
			UUID.fromString("00000000-0000-0000-0000-000000000001"),
			UUID.fromString("00000000-0000-0000-0000-000000000002"),
			"checkout-api",
			null,
			"Payment failures",
			AlertSeverity.ERROR,
			java.util.List.of(new AlertLogSample("ERROR", "Failure")),
			ts, ts, ts,
			Set.of(AlertDeliveryTarget.channelOnly(AlertChannel.WEBSOCKET)),
			1
		);
		UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000005");

		alert.acknowledge(userId);
		alert.resolve(userId);

		assertThat(alert.getStatus()).isEqualTo(AlertStatus.RESOLVED);
		assertThat(alert.getAcknowledgedBy()).isEqualTo(userId);
		assertThat(alert.getAcknowledgedAt()).isNotNull();
		assertThat(alert.getResolvedBy()).isEqualTo(userId);
		assertThat(alert.getResolvedAt()).isNotNull();
	}

	@Test
	void retriggerReopensAcknowledgedOccurrenceAndIncrementsCount() {
		Instant firstSeenAt = Instant.parse("2026-06-18T03:00:00Z");
		AlertEntity alert = AlertEntity.create(
			UUID.randomUUID(), UUID.randomUUID(),
			"checkout-api", null, "Payment failures", AlertSeverity.ERROR,
			java.util.List.of(new AlertLogSample("ERROR", "Failure")),
			firstSeenAt, firstSeenAt, firstSeenAt,
			Set.of(AlertDeliveryTarget.channelOnly(AlertChannel.WEBSOCKET)), 1);
		alert.acknowledge(UUID.randomUUID());
		Instant nextOccurrence = Instant.parse("2026-06-18T03:01:00Z");

		alert.retrigger(nextOccurrence);

		assertThat(alert.getStatus()).isEqualTo(AlertStatus.OPEN);
		assertThat(alert.getOccurrenceCount()).isEqualTo(2);
		assertThat(alert.getLastSeenAt()).isEqualTo(nextOccurrence);
		assertThat(alert.getAcknowledgedBy()).isNull();
		assertThat(alert.getAcknowledgedAt()).isNull();
	}
}
