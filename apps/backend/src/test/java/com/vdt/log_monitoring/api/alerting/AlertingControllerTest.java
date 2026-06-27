package com.vdt.log_monitoring.api.alerting;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.vdt.log_monitoring.modules.alerting.api.AlertingFacade;
import com.vdt.log_monitoring.modules.identity.api.IdentityFacade;
import com.vdt.log_monitoring.modules.identity.api.ApplicationAccessFacade;
import com.vdt.log_monitoring.shared.security.JwtTokenProvider;

@WebMvcTest({
		AlertRuleController.class,
		AlertController.class,
		AlertChatRoomController.class
})
@Import(AlertingExceptionHandler.class)
class AlertingControllerTest {

	private static final Instant NOW = Instant.parse("2026-06-18T04:00:00Z");
	private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID APP_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
	private static final UUID RULE_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");
	private static final UUID ALERT_ID = UUID.fromString("00000000-0000-0000-0000-000000000301");
	private static final UUID CHAT_ROOM_ID = UUID.fromString("00000000-0000-0000-0000-000000000401");

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private AlertingFacade alertingFacade;

	@MockBean
	private IdentityFacade identityFacade;

	@MockBean
	private ApplicationAccessFacade applicationAccessFacade;

	@MockBean
	private JwtTokenProvider jwtTokenProvider;

	@Test
	void adminCreatesAlertRuleWithDeliveryTarget() throws Exception {
		when(identityFacade.findUserByEmail("admin@example.com")).thenReturn(userDto());
		when(alertingFacade.createRule(any())).thenReturn(ruleDto());

		mockMvc.perform(post("/api/v1/alert-rules")
				.with(user("admin@example.com").roles("ADMIN"))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "applicationId": "00000000-0000-0000-0000-000000000101",
						  "name": "Critical checkout errors",
						  "description": "Notify checkout failures",
						  "minSeverity": "ERROR",
						  "severity": "CRITICAL",
						  "keywordPattern": "payment failed",
						  "thresholdCount": 3,
						  "thresholdWindowSeconds": 300,
						  "cooldownSeconds": 120,
						  "deliveryTargets": [
						    {
						      "channel": "TELEGRAM",
						      "chatRoomId": "00000000-0000-0000-0000-000000000401"
						    }
						  ]
						}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.deliveryTargets[0].chatRoomId").value(CHAT_ROOM_ID.toString()));

		ArgumentCaptor<AlertingFacade.CreateAlertRuleCommand> captor = ArgumentCaptor
				.forClass(AlertingFacade.CreateAlertRuleCommand.class);
		verify(alertingFacade).createRule(captor.capture());
		org.assertj.core.api.Assertions.assertThat(captor.getValue().createdBy()).isEqualTo(USER_ID);
		org.assertj.core.api.Assertions.assertThat(captor.getValue().deliveryTargets().getFirst().chatRoomId())
				.isEqualTo(CHAT_ROOM_ID);
	}

	@Test
	void adminListsAndCreatesChatRooms() throws Exception {
		when(identityFacade.findUserByEmail("admin@example.com")).thenReturn(userDto());
		when(alertingFacade.createChatRoom(any())).thenReturn(chatRoomDto());
		when(alertingFacade.findChatRooms("TELEGRAM", "ACTIVE")).thenReturn(List.of(chatRoomDto()));

		mockMvc.perform(post("/api/v1/alert-chat-rooms")
				.with(user("admin@example.com").roles("ADMIN"))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "channel": "TELEGRAM",
						  "name": "Ops critical",
						  "chatId": "-100123456",
						  "description": "Primary on-call room"
						}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.chatId").value("-100123456"));

		mockMvc.perform(get("/api/v1/alert-chat-rooms")
				.with(user("admin@example.com").roles("ADMIN"))
				.param("channel", "TELEGRAM")
				.param("status", "ACTIVE"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].id").value(CHAT_ROOM_ID.toString()));
	}

	@Test
	void adminDiscoversTelegramChatsForChatRoomDropdown() throws Exception {
		when(alertingFacade.discoverTelegramChats()).thenReturn(List.of(
				new AlertingFacade.TelegramChatDto("-100123456", "Ops critical", "SUPERGROUP", "ops_team")));

		mockMvc.perform(get("/api/v1/alert-chat-rooms/telegram/discover")
				.with(user("admin@example.com").roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].chatId").value("-100123456"))
				.andExpect(jsonPath("$.data[0].name").value("Ops critical"))
				.andExpect(jsonPath("$.data[0].type").value("SUPERGROUP"))
				.andExpect(jsonPath("$.data[0].username").value("ops_team"));
	}

	@Test
	void authenticatedUserAcknowledgesAlert() throws Exception {
		when(identityFacade.findUserByEmail("engineer@example.com")).thenReturn(userDto());
		when(applicationAccessFacade.findVisibleApplications(USER_ID, "ADMIN"))
				.thenReturn(List.of(applicationDto()));
		when(alertingFacade.findAlertById(ALERT_ID)).thenReturn(alertDto());
		when(alertingFacade.acknowledgeAlert(ALERT_ID, USER_ID)).thenReturn(alertDto());

		mockMvc.perform(put("/api/v1/alerts/{id}/acknowledge", ALERT_ID)
				.with(user("engineer@example.com").roles("ENGINEER"))
				.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("ACKNOWLEDGED"));
	}

	@Test
	void authenticatedUserListsAlertsFromVisibleApplications() throws Exception {
		when(identityFacade.findUserByEmail("engineer@example.com")).thenReturn(userDto());
		when(applicationAccessFacade.findVisibleApplications(USER_ID, "ADMIN"))
				.thenReturn(List.of(applicationDto()));
		when(alertingFacade.findAlerts(List.of(APP_ID), "OPEN", "ERROR"))
				.thenReturn(List.of(alertDto()));

		mockMvc.perform(get("/api/v1/alerts")
				.with(user("engineer@example.com").roles("ENGINEER"))
				.param("status", "OPEN")
				.param("severity", "ERROR"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].applicationId").value(APP_ID.toString()));

		verify(alertingFacade).findAlerts(List.of(APP_ID), "OPEN", "ERROR");
	}

	private static IdentityFacade.UserDto userDto() {
		return new IdentityFacade.UserDto(
				USER_ID,
				"admin@example.com",
				"Admin",
				"ADMIN",
				"ACTIVE",
				NOW,
				NOW,
				NOW);
	}

	private static ApplicationAccessFacade.ApplicationDto applicationDto() {
		return new ApplicationAccessFacade.ApplicationDto(
				APP_ID, "checkout-api", "Checkout API", null, "ACTIVE", NOW, NOW);
	}

	private static AlertingFacade.AlertRuleDto ruleDto() {
		return new AlertingFacade.AlertRuleDto(
				RULE_ID,
				APP_ID,
				"Critical checkout errors",
				"Notify checkout failures",
				"ERROR",
				"CRITICAL",
				"payment failed",
				3,
				300,
				120,
				"ACTIVE",
				List.of("TELEGRAM"),
				List.of(new AlertingFacade.AlertDeliveryTargetDto("TELEGRAM", CHAT_ROOM_ID)),
				USER_ID,
				NOW,
				NOW);
	}

	private static AlertingFacade.ChatRoomDto chatRoomDto() {
		return new AlertingFacade.ChatRoomDto(
				CHAT_ROOM_ID,
				"TELEGRAM",
				"Ops critical",
				"-100123456",
				"Primary on-call room",
				"ACTIVE",
				USER_ID,
				NOW,
				NOW);
	}

	private static AlertingFacade.AlertDto alertDto() {
		return new AlertingFacade.AlertDto(
				ALERT_ID,
				RULE_ID,
				"Checkout API error rate high",
				APP_ID,
				"checkout-api",
				"Checkout API",
				"ERROR",
				List.of(new com.vdt.log_monitoring.api.alerting.dto.AlertLogSampleDto("ERROR", "Payment failed")),
				NOW,
				5,
				NOW,
				NOW,
				"ACKNOWLEDGED",
				List.of("TELEGRAM"),
				List.of(new AlertingFacade.AlertDeliveryTargetDto("TELEGRAM", CHAT_ROOM_ID)),
				USER_ID,
				NOW,
				null,
				null,
				NOW,
				NOW);
	}
}
