package com.vdt.log_monitoring.modules.alerting.internal.notification.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.vdt.log_monitoring.modules.alerting.api.AlertingException;

class TelegramChatDiscoveryServiceTest {

	@Test
	void discoverChatsReturnsUniqueGroupsFromTelegramUpdates() {
		RestClient.Builder restClientBuilder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
		server.expect(request -> {
			assertThat(request.getURI().getPath()).isEqualTo("/bottest-token/getUpdates");
			assertThat(request.getURI().getQuery()).contains("timeout=0", "allowed_updates=");
		}).andRespond(withSuccess("""
			{
			  "ok": true,
			  "result": [
			    {
			      "update_id": 1,
			      "message": {
			        "chat": {"id": -1002, "type": "supergroup", "title": "Ops"}
			      }
			    },
			    {
			      "update_id": 2,
			      "my_chat_member": {
			        "chat": {"id": -1001, "type": "group", "title": "Developers", "username": "dev_team"}
			      }
			    },
			    {
			      "update_id": 3,
			      "edited_message": {
			        "chat": {"id": -1001, "type": "group", "title": "Developers"}
			      }
			    },
			    {
			      "update_id": 4,
			      "message": {
			        "chat": {"id": 42, "type": "private", "username": "someone"}
			      }
			    }
			  ]
			}
			""", MediaType.APPLICATION_JSON));
		TelegramChatDiscoveryService service = new TelegramChatDiscoveryService(
			restClientBuilder,
			"https://api.telegram.test",
			"test-token");

		List<TelegramChatDiscoveryService.DiscoveredTelegramChat> chats = service.discoverChats();

		assertThat(chats).containsExactly(
			new TelegramChatDiscoveryService.DiscoveredTelegramChat(
				"-1001", "Developers", "GROUP", "dev_team"),
			new TelegramChatDiscoveryService.DiscoveredTelegramChat(
				"-1002", "Ops", "SUPERGROUP", null));
		server.verify();
	}

	@Test
	void discoverChatsRejectsMissingBotToken() {
		TelegramChatDiscoveryService service = new TelegramChatDiscoveryService(
			RestClient.builder(),
			"https://api.telegram.test",
			" ");

		assertThatThrownBy(service::discoverChats)
			.isInstanceOfSatisfying(AlertingException.class, exception ->
				assertThat(exception.getErrorCode())
					.isEqualTo(AlertingException.ErrorCode.TELEGRAM_NOT_CONFIGURED));
	}
}
