package com.vdt.log_monitoring.modules.alerting.internal.rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.vdt.log_monitoring.modules.alerting.api.AlertingFacade.AlertDeliveryTargetCommand;
import com.vdt.log_monitoring.modules.alerting.internal.cache.AlertRuleCache;
import com.vdt.log_monitoring.modules.alerting.internal.notification.ChatRoomEntity;
import com.vdt.log_monitoring.modules.alerting.internal.notification.ChatRoomRepository;
import com.vdt.log_monitoring.modules.identity.api.ApplicationAccessFacade;

class AlertRuleServiceTest {

	@Test
	void createRuleAcceptsMultipleTelegramChatRooms() {
		AlertRuleRepository ruleRepository = mock();
		ChatRoomRepository chatRoomRepository = mock();
		ApplicationAccessFacade applicationAccessFacade = mock();
		AlertRuleCache ruleCache = mock();
		AlertDeliveryTargetResolver targetResolver = new AlertDeliveryTargetResolver(chatRoomRepository);
		AlertRuleService service = new AlertRuleService(
			ruleRepository, applicationAccessFacade, ruleCache, targetResolver);
		UUID applicationId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		UUID firstChatRoomId = UUID.fromString("00000000-0000-0000-0000-000000000011");
		UUID secondChatRoomId = UUID.fromString("00000000-0000-0000-0000-000000000012");
		ChatRoomEntity firstChat = ChatRoomEntity.create(
			AlertChannel.TELEGRAM, "Primary", "-100001", null, UUID.randomUUID());
		ChatRoomEntity secondChat = ChatRoomEntity.create(
			AlertChannel.TELEGRAM, "Backup", "-100002", null, UUID.randomUUID());
		org.springframework.test.util.ReflectionTestUtils.setField(firstChat, "id", firstChatRoomId);
		org.springframework.test.util.ReflectionTestUtils.setField(secondChat, "id", secondChatRoomId);
		when(chatRoomRepository.findById(firstChatRoomId)).thenReturn(Optional.of(firstChat));
		when(chatRoomRepository.findById(secondChatRoomId)).thenReturn(Optional.of(secondChat));
		when(ruleRepository.save(any(AlertRuleEntity.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		AlertRuleEntity rule = service.createRule(
			applicationId, "Payment failures", null, "ERROR", "payment",
			1, 60, 60, List.of("WEBSOCKET"),
			List.of(
				new AlertDeliveryTargetCommand("TELEGRAM", firstChatRoomId),
				new AlertDeliveryTargetCommand("TELEGRAM", secondChatRoomId)),
			UUID.fromString("00000000-0000-0000-0000-000000000002"));

		assertThat(rule.getDeliveryTargets())
			.hasSize(3);
		assertThat(rule.getDeliveryTargets())
			.filteredOn(target -> target.getChannel() == AlertChannel.TELEGRAM)
			.extracting(AlertDeliveryTarget::getChatRoomId)
			.containsExactlyInAnyOrder(firstChatRoomId, secondChatRoomId);
	}
}
