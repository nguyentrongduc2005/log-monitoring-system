package com.vdt.log_monitoring.api.identity;

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

import com.vdt.log_monitoring.modules.identity.api.ApplicationAccessFacade;
import com.vdt.log_monitoring.modules.identity.api.IdentityFacade;
import com.vdt.log_monitoring.shared.security.JwtTokenProvider;

@WebMvcTest(ApplicationController.class)
@Import(IdentityExceptionHandler.class)
class ApplicationControllerTest {

	private static final Instant NOW = Instant.parse("2026-06-10T06:00:00Z");
	private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID ENGINEER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
	private static final UUID APP_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ApplicationAccessFacade applicationAccessFacade;

	@MockBean
	private IdentityFacade identityFacade;

	@MockBean
	private JwtTokenProvider jwtTokenProvider;

	@Test
	void adminCreatesApplication() throws Exception {
		when(identityFacade.findUserByEmail("admin@example.com")).thenReturn(userDto(ADMIN_ID, "ADMIN"));
		when(applicationAccessFacade.createApplication(any())).thenReturn(applicationDto("gateway"));

		mockMvc.perform(post("/api/v1/applications")
				.with(user("admin@example.com").roles("ADMIN"))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "name": "gateway",
					  "displayName": "Gateway",
					  "description": "Edge service"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.name").value("gateway"));

		ArgumentCaptor<ApplicationAccessFacade.CreateApplicationCommand> captor =
			ArgumentCaptor.forClass(ApplicationAccessFacade.CreateApplicationCommand.class);
		verify(applicationAccessFacade).createApplication(captor.capture());
		org.assertj.core.api.Assertions.assertThat(captor.getValue().createdBy()).isEqualTo(ADMIN_ID);
	}

	@Test
	void adminListsAndReadsApplications() throws Exception {
		when(applicationAccessFacade.findAllApplications()).thenReturn(List.of(applicationDto("gateway")));
		when(applicationAccessFacade.findApplicationById(APP_ID)).thenReturn(applicationDto("gateway"));

		mockMvc.perform(get("/api/v1/applications").with(user("admin@example.com").roles("ADMIN")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(1));

		mockMvc.perform(get("/api/v1/applications/{id}", APP_ID).with(user("admin@example.com").roles("ADMIN")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.id").value(APP_ID.toString()));
	}

	@Test
	void adminUpdatesApplicationAndStatus() throws Exception {
		when(applicationAccessFacade.updateApplication(any(), any())).thenReturn(applicationDto("gateway-v2"));
		when(applicationAccessFacade.changeApplicationStatus(APP_ID, "INACTIVE"))
			.thenReturn(new ApplicationAccessFacade.ApplicationDto(
				APP_ID, "gateway", "Gateway", null, "INACTIVE", NOW, NOW
			));

		mockMvc.perform(put("/api/v1/applications/{id}", APP_ID)
				.with(user("admin@example.com").roles("ADMIN"))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "name": "gateway-v2",
					  "displayName": "Gateway V2"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.name").value("gateway-v2"));

		mockMvc.perform(put("/api/v1/applications/{id}/status", APP_ID)
				.with(user("admin@example.com").roles("ADMIN"))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"INACTIVE\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.status").value("INACTIVE"));
	}

	@Test
	void authenticatedUserGetsVisibleApplications() throws Exception {
		when(identityFacade.findUserByEmail("engineer@example.com")).thenReturn(userDto(ENGINEER_ID, "ENGINEER"));
		when(applicationAccessFacade.findVisibleApplications(ENGINEER_ID, "ENGINEER"))
			.thenReturn(List.of(applicationDto("gateway")));

		mockMvc.perform(get("/api/v1/applications/me")
				.with(user("engineer@example.com").roles("ENGINEER")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[0].name").value("gateway"));
	}

	private static IdentityFacade.UserDto userDto(UUID id, String role) {
		return new IdentityFacade.UserDto(id, role.toLowerCase() + "@example.com", role, role, "ACTIVE", NOW, NOW, NOW);
	}

	private static ApplicationAccessFacade.ApplicationDto applicationDto(String name) {
		return new ApplicationAccessFacade.ApplicationDto(APP_ID, name, "Gateway", null, "ACTIVE", NOW, NOW);
	}
}
