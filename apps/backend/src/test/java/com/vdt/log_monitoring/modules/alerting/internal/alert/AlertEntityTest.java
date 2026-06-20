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
			UUID.fromString("00000000-0000-0000-0000-000000000003"),
			UUID.fromString("00000000-0000-0000-0000-000000000004"),
			" checkout-api ",
			" Checkout API ",
			AlertSeverity.CRITICAL,
			" Payment failure spike ",
			" checkout-payment-failure ",
			logTimestamp,
			Set.of(
				AlertDeliveryTarget.channelOnly(AlertChannel.TELEGRAM),
				AlertDeliveryTarget.channelOnly(AlertChannel.WEBSOCKET)
			)
		);

		assertThat(alert.getId()).isNotNull();
		assertThat(alert.getApplicationName()).isEqualTo("checkout-api");
		assertThat(alert.getApplicationDisplayName()).isEqualTo("Checkout API");
		assertThat(alert.getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
		assertThat(alert.getMessage()).isEqualTo("Payment failure spike");
		assertThat(alert.getFingerprint()).isEqualTo("checkout-payment-failure");
		assertThat(alert.getLogTimestamp()).isEqualTo(logTimestamp);
		assertThat(alert.getTriggeredAt()).isNotNull();
		assertThat(alert.getStatus()).isEqualTo(AlertStatus.OPEN);
		assertThat(alert.getDeliveryChannels()).containsExactlyInAnyOrder(
			AlertChannel.TELEGRAM,
			AlertChannel.WEBSOCKET
		);
	}

	@Test
	void acknowledgeAndResolveAlert() {
		AlertEntity alert = AlertEntity.create(
			UUID.fromString("00000000-0000-0000-0000-000000000001"),
			UUID.fromString("00000000-0000-0000-0000-000000000002"),
			UUID.fromString("00000000-0000-0000-0000-000000000003"),
			UUID.fromString("00000000-0000-0000-0000-000000000004"),
			"checkout-api",
			null,
			AlertSeverity.ERROR,
			"Failure",
			"failure",
			Instant.parse("2026-06-18T03:00:00Z"),
			Set.of(AlertDeliveryTarget.channelOnly(AlertChannel.WEBSOCKET))
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
}
