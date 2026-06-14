package com.vdt.log_monitoring.api.identity;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.vdt.log_monitoring.modules.identity.api.IdentityFacade;
import com.vdt.log_monitoring.modules.identity.api.IdentityException;
import com.vdt.log_monitoring.shared.security.JwtTokenProvider;

@WebMvcTest(UserAdminController.class)
@Import(IdentityExceptionHandler.class)
class UserAdminControllerTest {

	private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final Instant NOW = Instant.parse("2026-06-10T06:00:00Z");

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private IdentityFacade identityFacade;

	@MockBean
	private JwtTokenProvider jwtTokenProvider;

	@Test
	void adminCreatesUser() throws Exception {
		when(identityFacade.createUser(
			"engineer@example.com",
			"password",
			"Engineer",
			"ENGINEER"
		)).thenReturn(userDto("engineer@example.com", "Engineer", "ENGINEER"));

		mockMvc.perform(post("/api/v1/users")
				.with(user("admin@example.com").roles("ADMIN"))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "email": "engineer@example.com",
					  "password": "password",
					  "displayName": "Engineer",
					  "role": "ENGINEER"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.email").value("engineer@example.com"))
			.andExpect(jsonPath("$.data.role").value("ENGINEER"));
	}

	@Test
	void adminListsUsers() throws Exception {
		when(identityFacade.findUsers(any())).thenReturn(new IdentityFacade.UserPageDto(
			List.of(
				userDto("admin@example.com", "Admin", "ADMIN"),
				userDto("engineer@example.com", "Engineer", "ENGINEER")
			),
			0,
			20,
			2,
			1
		));

		mockMvc.perform(get("/api/v1/users")
				.param("search", "eng")
				.param("role", "ENGINEER")
				.param("status", "ACTIVE")
				.with(user("admin@example.com").roles("ADMIN")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.users.length()").value(2))
			.andExpect(jsonPath("$.data.totalElements").value(2));
	}

	@Test
	void adminGetsUserById() throws Exception {
		when(identityFacade.findUserById(USER_ID)).thenReturn(userDto("engineer@example.com", "Engineer", "ENGINEER"));

		mockMvc.perform(get("/api/v1/users/{id}", USER_ID)
				.with(user("admin@example.com").roles("ADMIN")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.id").value(USER_ID.toString()));
	}

	@Test
	void adminChangesRole() throws Exception {
		when(identityFacade.changeRole(USER_ID, "ADMIN")).thenReturn(userDto("engineer@example.com", "Engineer", "ADMIN"));

		mockMvc.perform(put("/api/v1/users/{id}/role", USER_ID)
				.with(user("admin@example.com").roles("ADMIN"))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("\"ADMIN\""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.role").value("ADMIN"));
	}

	@Test
	void adminChangesStatus() throws Exception {
		when(identityFacade.changeStatus(USER_ID, "DISABLED"))
			.thenReturn(new IdentityFacade.UserDto(
				USER_ID,
				"engineer@example.com",
				"Engineer",
				"ENGINEER",
				"DISABLED",
				NOW,
				NOW,
				NOW
			));

		mockMvc.perform(put("/api/v1/users/{id}/status", USER_ID)
				.with(user("admin@example.com").roles("ADMIN"))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("\"DISABLED\""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.status").value("DISABLED"));
	}

	@Test
	void adminUpdatesAndSoftDeletesUser() throws Exception {
		when(identityFacade.updateUser(USER_ID, "updated@example.com", "Updated"))
			.thenReturn(userDto("updated@example.com", "Updated", "ENGINEER"));
		when(identityFacade.softDeleteUser(USER_ID))
			.thenReturn(new IdentityFacade.UserDto(
				USER_ID,
				"updated@example.com",
				"Updated",
				"ENGINEER",
				"DELETED",
				NOW,
				NOW,
				NOW
			));

		mockMvc.perform(put("/api/v1/users/{id}", USER_ID)
				.with(user("admin@example.com").roles("ADMIN"))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "email": "updated@example.com",
					  "displayName": "Updated"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.email").value("updated@example.com"));

		mockMvc.perform(delete("/api/v1/users/{id}", USER_ID)
				.with(user("admin@example.com").roles("ADMIN"))
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.status").value("DELETED"));
	}

	@Test
	void duplicateEmailMapsToConflict() throws Exception {
		when(identityFacade.createUser(anyString(), anyString(), anyString(), anyString()))
			.thenThrow(new IdentityException(
				IdentityException.ErrorCode.EMAIL_ALREADY_EXISTS,
				"Email is already registered"
			));

		mockMvc.perform(post("/api/v1/users")
				.with(user("admin@example.com").roles("ADMIN"))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "email": "engineer@example.com",
					  "password": "password",
					  "displayName": "Engineer",
					  "role": "ENGINEER"
					}
					"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.data").value("EMAIL_ALREADY_EXISTS"));
	}

	private static IdentityFacade.UserDto userDto(String email, String displayName, String role) {
		return new IdentityFacade.UserDto(
			USER_ID,
			email,
			displayName,
			role,
			"ACTIVE",
			NOW,
			NOW,
			NOW
		);
	}
}
