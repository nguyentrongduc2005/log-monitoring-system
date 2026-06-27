package com.vdt.log_monitoring.modules.alerting.internal.rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class AlertRuleEntityTest {

	@Test
	void createTrimsFieldsAndStoresSelectedChannels() {
		UUID applicationId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		UUID createdBy = UUID.fromString("00000000-0000-0000-0000-000000000002");

		AlertRuleEntity rule = AlertRuleEntity.create(
			applicationId,
			" Critical checkout errors ",
			" Notify checkout failures ",
			AlertSeverity.ERROR,
			AlertSeverity.ERROR,
			" payment failed ",
			3,
			300,
			120,
			AlertRuleEntity.channelOnlyTargets(Set.of(AlertChannel.TELEGRAM, AlertChannel.WEBSOCKET)),
			createdBy
		);

		assertThat(rule.getId()).isNotNull();
		assertThat(rule.getApplicationId()).isEqualTo(applicationId);
		assertThat(rule.getName()).isEqualTo("Critical checkout errors");
		assertThat(rule.getDescription()).isEqualTo("Notify checkout failures");
		assertThat(rule.getMinSeverity()).isEqualTo(AlertSeverity.ERROR);
		assertThat(rule.getKeywordPattern()).isEqualTo("payment failed");
		assertThat(rule.getThresholdCount()).isEqualTo(3);
		assertThat(rule.getThresholdWindowSeconds()).isEqualTo(300);
		assertThat(rule.getCooldownSeconds()).isEqualTo(120);
		assertThat(rule.getStatus()).isEqualTo(AlertRuleStatus.ACTIVE);
		assertThat(rule.getChannels()).containsExactlyInAnyOrder(
			AlertChannel.TELEGRAM,
			AlertChannel.WEBSOCKET
		);
		assertThat(rule.getDeliveryTargets())
			.extracting(AlertDeliveryTarget::getChatRoomId)
			.containsOnlyNulls();
		assertThat(rule.getCreatedBy()).isEqualTo(createdBy);
		assertThat(rule.getCreatedAt()).isNotNull();
		assertThat(rule.getUpdatedAt()).isNotNull();
	}

	@Test
	void updateRuleCanSwitchToSingleChannel() {
		AlertRuleEntity rule = AlertRuleEntity.create(
			UUID.fromString("00000000-0000-0000-0000-000000000001"),
			"Critical checkout errors",
			null,
			AlertSeverity.ERROR,
			AlertSeverity.ERROR,
			null,
			1,
			60,
			60,
			AlertRuleEntity.channelOnlyTargets(Set.of(AlertChannel.WEBSOCKET)),
			UUID.fromString("00000000-0000-0000-0000-000000000002")
		);

		rule.updateRule(
			" Critical logs ",
			" Telegram only ",
			AlertSeverity.CRITICAL,
			AlertSeverity.CRITICAL,
			" OutOfMemory ",
			2,
			120,
			300,
			AlertRuleEntity.channelOnlyTargets(Set.of(AlertChannel.TELEGRAM))
		);
		rule.changeStatus(AlertRuleStatus.DISABLED);

		assertThat(rule.getName()).isEqualTo("Critical logs");
		assertThat(rule.getDescription()).isEqualTo("Telegram only");
		assertThat(rule.getMinSeverity()).isEqualTo(AlertSeverity.CRITICAL);
		assertThat(rule.getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
		assertThat(rule.getKeywordPattern()).isEqualTo("OutOfMemory");
		assertThat(rule.getThresholdCount()).isEqualTo(2);
		assertThat(rule.getThresholdWindowSeconds()).isEqualTo(120);
		assertThat(rule.getCooldownSeconds()).isEqualTo(300);
		assertThat(rule.getChannels()).containsExactly(AlertChannel.TELEGRAM);
		assertThat(rule.getStatus()).isEqualTo(AlertRuleStatus.DISABLED);
	}

	@Test
	void createRequiresAtLeastOneChannel() {
		assertThatThrownBy(() -> AlertRuleEntity.create(
			UUID.fromString("00000000-0000-0000-0000-000000000001"),
			"Critical checkout errors",
			null,
			AlertSeverity.ERROR,
			AlertSeverity.ERROR,
			null,
			1,
			60,
			60,
			Set.of(),
			UUID.fromString("00000000-0000-0000-0000-000000000002")
		)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("deliveryTargets must not be empty");
	}

	@Test
	void createAllowsMultipleTelegramChatRooms() {
		UUID firstChatRoomId = UUID.fromString("00000000-0000-0000-0000-000000000011");
		UUID secondChatRoomId = UUID.fromString("00000000-0000-0000-0000-000000000012");

		AlertRuleEntity rule = AlertRuleEntity.create(
			UUID.fromString("00000000-0000-0000-0000-000000000001"),
			"Critical checkout errors",
			null,
			AlertSeverity.ERROR,
			AlertSeverity.ERROR,
			null,
			1,
			60,
			60,
			Set.of(
				AlertDeliveryTarget.of(AlertChannel.TELEGRAM, firstChatRoomId),
				AlertDeliveryTarget.of(AlertChannel.TELEGRAM, secondChatRoomId),
				AlertDeliveryTarget.channelOnly(AlertChannel.WEBSOCKET)),
			UUID.fromString("00000000-0000-0000-0000-000000000002"));

		assertThat(rule.getDeliveryTargets()).hasSize(3);
		assertThat(rule.getDeliveryTargets())
			.filteredOn(target -> target.getChannel() == AlertChannel.TELEGRAM)
			.extracting(AlertDeliveryTarget::getChatRoomId)
			.containsExactlyInAnyOrder(firstChatRoomId, secondChatRoomId);
	}
}
