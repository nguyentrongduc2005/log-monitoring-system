package com.vdt.log_monitoring.shared.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.vdt.log_monitoring.api.identity.UserController;
import com.vdt.log_monitoring.modules.identity.api.IdentityFacade;

@WebMvcTest(UserController.class)
@Import({
	SecurityConfig.class,
	JwtAuthenticationFilter.class,
	SecurityConfigTest.TestSecurityBeans.class
})
class SecurityConfigTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private IdentityFacade identityFacade;

	@Test
	void engineerCanReadOwnProfile() throws Exception {
		mockMvc.perform(get("/api/v1/users/me")
				.with(user("engineer@example.com").roles("ENGINEER")))
			.andExpect(status().isOk());
	}

	@Test
	void engineerCanUpdateOwnProfile() throws Exception {
		mockMvc.perform(put("/api/v1/users/me")
				.with(user("engineer@example.com").roles("ENGINEER"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "email": "new@example.com",
					  "displayName": "New Name"
					}
					"""))
			.andExpect(status().isOk());
	}

	@Test
	void engineerCanChangeOwnPassword() throws Exception {
		mockMvc.perform(put("/api/v1/users/me/password")
				.with(user("engineer@example.com").roles("ENGINEER"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "oldPassword": "old-password",
					  "newPassword": "new-password"
					}
					"""))
			.andExpect(status().isOk());
	}

	@Test
	void engineerCannotAccessAdminUserManagement() throws Exception {
		mockMvc.perform(get("/api/v1/users/{id}", UUID.randomUUID())
				.with(user("engineer@example.com").roles("ENGINEER")))
			.andExpect(status().isForbidden());
	}

	@Test
	void adminCanAccessAdminUserManagement() throws Exception {
		mockMvc.perform(get("/api/v1/users/{id}", UUID.randomUUID())
				.with(user("admin@example.com").roles("ADMIN")))
			.andExpect(status().isOk());
	}

	private static IdentityFacade.UserDto userDto() {
		Instant now = Instant.parse("2026-06-09T10:00:00Z");
		return new IdentityFacade.UserDto(
			UUID.fromString("00000000-0000-0000-0000-000000000001"),
			"engineer@example.com",
			"Engineer",
			"ENGINEER",
			"ACTIVE",
			now,
			now,
			now
		);
	}

	@TestConfiguration
	static class TestSecurityBeans {

		@Bean
		JwtTokenProvider jwtTokenProvider() {
			return new JwtTokenProvider(
				"default-secret-key-that-must-be-very-long-and-secure-for-hmac-sha-256",
				86_400_000L
			);
		}

		@Bean
		IdentityFacade identityFacade() {
			return new StubIdentityFacade();
		}
	}

	static class StubIdentityFacade implements IdentityFacade {

		private final UserDto baseUser = userDto();

		@Override
		public UserDto createUser(String email, String rawPassword, String displayName, String role) {
			throw new UnsupportedOperationException("Not needed for security matcher tests");
		}

		@Override
		public UserDto findUserById(UUID id) {
			return new UserDto(
				id,
				"admin@example.com",
				"Admin",
				"ADMIN",
				"ACTIVE",
				baseUser.lastLoginAt(),
				baseUser.createdAt(),
				baseUser.updatedAt()
			);
		}

		@Override
		public UserDto findUserByEmail(String email) {
			return new UserDto(
				baseUser.id(),
				email,
				"Engineer",
				"ENGINEER",
				"ACTIVE",
				baseUser.lastLoginAt(),
				baseUser.createdAt(),
				baseUser.updatedAt()
			);
		}

		@Override
		public UserDto updateProfile(UUID id, String email, String displayName) {
			return new UserDto(
				id,
				email,
				displayName,
				"ENGINEER",
				"ACTIVE",
				baseUser.lastLoginAt(),
				baseUser.createdAt(),
				Instant.parse("2026-06-09T11:00:00Z")
			);
		}

		@Override
		public void changePassword(UUID id, String oldPassword, String newPassword) {
			// No-op for matcher tests.
		}

		@Override
		public UserDto changeRole(UUID id, String role) {
			throw new UnsupportedOperationException("Not needed for security matcher tests");
		}

		@Override
		public UserDto changeStatus(UUID id, String status) {
			throw new UnsupportedOperationException("Not needed for security matcher tests");
		}

		@Override
		public TokenPairDto authenticate(String email, String rawPassword) {
			throw new UnsupportedOperationException("Not needed for security matcher tests");
		}

		@Override
		public TokenPairDto refresh(String refreshToken) {
			throw new UnsupportedOperationException("Not needed for security matcher tests");
		}
	}
}
