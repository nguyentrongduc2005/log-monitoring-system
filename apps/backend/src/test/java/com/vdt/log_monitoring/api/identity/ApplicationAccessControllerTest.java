package com.vdt.log_monitoring.api.identity;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

@WebMvcTest(ApplicationAccessController.class)
@Import(IdentityExceptionHandler.class)
class ApplicationAccessControllerTest {

	private static final Instant NOW = Instant.parse("2026-06-10T06:00:00Z");
	private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
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
	void adminGetsUserApplicationAccess() throws Exception {
		when(applicationAccessFacade.findUserApplicationAccess(USER_ID)).thenReturn(List.of(accessDto("VIEW")));

		mockMvc.perform(get("/api/v1/users/{userId}/applications", USER_ID)
				.with(user("admin@example.com").roles("ADMIN")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[0].accessLevel").value("VIEW"));
	}

	@Test
	void adminReplacesUserApplicationAccess() throws Exception {
		when(identityFacade.findUserByEmail("admin@example.com")).thenReturn(adminDto());
		when(applicationAccessFacade.replaceUserApplicationAccess(any(), any(), any()))
			.thenReturn(List.of(accessDto("MANAGE")));

		mockMvc.perform(put("/api/v1/users/{userId}/applications", USER_ID)
				.with(user("admin@example.com").roles("ADMIN"))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "grants": [
					    { "applicationId": "00000000-0000-0000-0000-000000000101", "accessLevel": "MANAGE" }
					  ]
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[0].accessLevel").value("MANAGE"));

		ArgumentCaptor<UUID> grantedByCaptor = ArgumentCaptor.forClass(UUID.class);
		verify(applicationAccessFacade).replaceUserApplicationAccess(
			org.mockito.Mockito.eq(USER_ID),
			any(),
			grantedByCaptor.capture()
		);
		org.assertj.core.api.Assertions.assertThat(grantedByCaptor.getValue()).isEqualTo(ADMIN_ID);
	}

	@Test
	void adminGrantsAndRemovesSingleApplicationAccess() throws Exception {
		when(identityFacade.findUserByEmail("admin@example.com")).thenReturn(adminDto());
		when(applicationAccessFacade.grantUserApplicationAccess(USER_ID, APP_ID, "VIEW", ADMIN_ID))
			.thenReturn(accessDto("VIEW"));

		mockMvc.perform(post("/api/v1/users/{userId}/applications/{applicationId}", USER_ID, APP_ID)
				.with(user("admin@example.com").roles("ADMIN"))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"accessLevel\":\"VIEW\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.accessLevel").value("VIEW"));

		mockMvc.perform(delete("/api/v1/users/{userId}/applications/{applicationId}", USER_ID, APP_ID)
				.with(user("admin@example.com").roles("ADMIN"))
				.with(csrf()))
			.andExpect(status().isOk());

		verify(applicationAccessFacade).removeUserApplicationAccess(USER_ID, APP_ID);
	}

	private static IdentityFacade.UserDto adminDto() {
		return new IdentityFacade.UserDto(ADMIN_ID, "admin@example.com", "Admin", "ADMIN", "ACTIVE", NOW, NOW, NOW);
	}

	private static ApplicationAccessFacade.ApplicationAccessDto accessDto(String level) {
		return new ApplicationAccessFacade.ApplicationAccessDto(
			USER_ID, APP_ID, "gateway", "Gateway", level, ADMIN_ID, NOW, NOW
		);
	}
}
