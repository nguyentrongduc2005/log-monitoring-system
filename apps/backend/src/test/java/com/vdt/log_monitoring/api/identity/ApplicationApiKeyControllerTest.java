package com.vdt.log_monitoring.api.identity;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.vdt.log_monitoring.modules.identity.api.ApplicationAccessFacade;
import com.vdt.log_monitoring.modules.identity.api.IdentityFacade;
import com.vdt.log_monitoring.shared.security.JwtTokenProvider;

@WebMvcTest(ApplicationApiKeyController.class)
@Import(IdentityExceptionHandler.class)
class ApplicationApiKeyControllerTest {

	private static final Instant NOW = Instant.parse("2026-06-10T06:00:00Z");
	private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID APP_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
	private static final UUID KEY_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ApplicationAccessFacade applicationAccessFacade;

	@MockBean
	private IdentityFacade identityFacade;

	@MockBean
	private JwtTokenProvider jwtTokenProvider;

	@Test
	void adminCreatesApiKeyAndGetsRawKeyOnce() throws Exception {
		when(identityFacade.findUserByEmail("admin@example.com")).thenReturn(adminDto());
		when(applicationAccessFacade.createApiKey(APP_ID, "ingest", null, ADMIN_ID))
			.thenReturn(apiKeyCreationDto("lms_live_abc.raw-secret"));

		mockMvc.perform(post("/api/v1/applications/{applicationId}/api-keys", APP_ID)
				.with(user("admin@example.com").roles("ADMIN"))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"ingest\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.rawApiKey").value("lms_live_abc.raw-secret"));
	}

	@Test
	void adminListsApiKeysWithoutRawKey() throws Exception {
		when(applicationAccessFacade.findApiKeys(APP_ID)).thenReturn(List.of(apiKeyDto()));

		mockMvc.perform(get("/api/v1/applications/{applicationId}/api-keys", APP_ID)
				.with(user("admin@example.com").roles("ADMIN")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[0].keyPrefix").value("lms_live_abc"))
			.andExpect(jsonPath("$.data[0].rawApiKey").doesNotExist());
	}

	@Test
	void adminRotatesAndRevokesApiKey() throws Exception {
		when(identityFacade.findUserByEmail("admin@example.com")).thenReturn(adminDto());
		when(applicationAccessFacade.rotateApiKey(APP_ID, KEY_ID, ADMIN_ID))
			.thenReturn(apiKeyCreationDto("lms_live_new.raw-secret"));

		mockMvc.perform(post("/api/v1/applications/{applicationId}/api-keys/{apiKeyId}/rotate", APP_ID, KEY_ID)
				.with(user("admin@example.com").roles("ADMIN"))
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.rawApiKey").value("lms_live_new.raw-secret"));

		mockMvc.perform(post("/api/v1/applications/{applicationId}/api-keys/{apiKeyId}/revoke", APP_ID, KEY_ID)
				.with(user("admin@example.com").roles("ADMIN"))
				.with(csrf()))
			.andExpect(status().isOk());

		verify(applicationAccessFacade).revokeApiKey(APP_ID, KEY_ID, ADMIN_ID);
	}

	private static IdentityFacade.UserDto adminDto() {
		return new IdentityFacade.UserDto(ADMIN_ID, "admin@example.com", "Admin", "ADMIN", "ACTIVE", NOW, NOW, NOW);
	}

	private static ApplicationAccessFacade.ApiKeyDto apiKeyDto() {
		return new ApplicationAccessFacade.ApiKeyDto(
			KEY_ID, APP_ID, "ingest", "lms_live_abc", "ACTIVE", null, null, NOW, null
		);
	}

	private static ApplicationAccessFacade.ApiKeyCreationDto apiKeyCreationDto(String rawKey) {
		return new ApplicationAccessFacade.ApiKeyCreationDto(
			KEY_ID, APP_ID, "ingest", "lms_live_abc", rawKey, "ACTIVE", null, NOW
		);
	}
}
