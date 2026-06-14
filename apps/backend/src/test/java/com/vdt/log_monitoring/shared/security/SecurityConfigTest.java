package com.vdt.log_monitoring.shared.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
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
import com.vdt.log_monitoring.api.identity.UserAdminController;
import com.vdt.log_monitoring.api.identity.ApplicationAccessController;
import com.vdt.log_monitoring.api.identity.ApplicationApiKeyController;
import com.vdt.log_monitoring.api.identity.ApplicationController;
import com.vdt.log_monitoring.modules.identity.api.ApplicationAccessFacade;
import com.vdt.log_monitoring.modules.identity.api.IdentityFacade;

@WebMvcTest({
	UserController.class,
	UserAdminController.class,
	ApplicationController.class,
	ApplicationAccessController.class,
	ApplicationApiKeyController.class
})
@Import({
	SecurityConfig.class,
	JwtAuthenticationFilter.class,
	SecurityConfigTest.TestSecurityBeans.class
})
class SecurityConfigTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void malformedJwtReturnsApiResponse() throws Exception {
		mockMvc.perform(get("/api/v1/users/me")
				.header("Authorization", "Bearer not-a-jwt"))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.message").value("Invalid JWT token"))
			.andExpect(jsonPath("$.data").value("INVALID_JWT"));
	}

	@Test
	void expiredJwtReturnsApiResponse() throws Exception {
		JwtTokenProvider expiredTokenProvider = new JwtTokenProvider(
			"default-secret-key-that-must-be-very-long-and-secure-for-hmac-sha-256",
			-1_000L
		);
		String expiredToken = expiredTokenProvider.generateToken(
			"engineer@example.com",
			"ENGINEER",
			"Engineer",
			UUID.randomUUID().toString()
		);

		mockMvc.perform(get("/api/v1/users/me")
				.header("Authorization", "Bearer " + expiredToken))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.message").value("JWT token has expired"))
			.andExpect(jsonPath("$.data").value("JWT_EXPIRED"));
	}

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
				.with(csrf())
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
				.with(csrf())
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

	@Test
	void engineerCanAccessVisibleApplications() throws Exception {
		mockMvc.perform(get("/api/v1/applications/me")
				.with(user("engineer@example.com").roles("ENGINEER")))
			.andExpect(status().isOk());
	}

	@Test
	void engineerCannotAccessApplicationAdministration() throws Exception {
		UUID applicationId = UUID.randomUUID();

		mockMvc.perform(get("/api/v1/applications")
				.with(user("engineer@example.com").roles("ENGINEER")))
			.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/v1/applications")
				.with(user("engineer@example.com").roles("ENGINEER"))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "name": "gateway",
					  "displayName": "Gateway"
					}
					"""))
			.andExpect(status().isForbidden());

		mockMvc.perform(put("/api/v1/users/{userId}/applications", UUID.randomUUID())
				.with(user("engineer@example.com").roles("ENGINEER"))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"grants\":[]}"))
			.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/v1/applications/{applicationId}/api-keys", applicationId)
				.with(user("engineer@example.com").roles("ENGINEER"))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"ingest\"}"))
			.andExpect(status().isForbidden());
	}

	@Test
	void adminCanAccessApplicationAdministration() throws Exception {
		mockMvc.perform(get("/api/v1/applications")
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

		@Bean
		ApplicationAccessFacade applicationAccessFacade() {
			return new StubApplicationAccessFacade();
		}
	}

	static class StubIdentityFacade implements IdentityFacade {

		private final UserDto baseUser = userDto();

		@Override
		public UserDto createUser(String email, String rawPassword, String displayName, String role) {
			throw new UnsupportedOperationException("Not needed for security matcher tests");
		}

		@Override
		public List<UserDto> findUsers() {
			return List.of(baseUser);
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

		@Override
		public void logout(String accessToken, String refreshToken) {
			throw new UnsupportedOperationException("Not needed for security matcher tests");
		}
	}

	static class StubApplicationAccessFacade implements ApplicationAccessFacade {

		private static final UUID APPLICATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
		private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
		private static final Instant NOW = Instant.parse("2026-06-09T10:00:00Z");

		@Override
		public ApplicationAccessFacade.ApplicationDto createApplication(
			ApplicationAccessFacade.CreateApplicationCommand command
		) {
			return applicationDto();
		}

		@Override
		public ApplicationAccessFacade.ApplicationDto updateApplication(
			UUID applicationId,
			ApplicationAccessFacade.UpdateApplicationCommand command
		) {
			return applicationDto();
		}

		@Override
		public ApplicationAccessFacade.ApplicationDto changeApplicationStatus(UUID applicationId, String status) {
			return new ApplicationAccessFacade.ApplicationDto(
				APPLICATION_ID, "gateway", "Gateway", null, status, NOW, NOW
			);
		}

		@Override
		public ApplicationAccessFacade.ApplicationDto findApplicationById(UUID applicationId) {
			return applicationDto();
		}

		@Override
		public List<ApplicationAccessFacade.ApplicationDto> findAllApplications() {
			return List.of(applicationDto());
		}

		@Override
		public List<ApplicationAccessFacade.ApplicationDto> findVisibleApplications(UUID userId, String role) {
			return List.of(applicationDto());
		}

		@Override
		public List<ApplicationAccessFacade.ApplicationAccessDto> findUserApplicationAccess(UUID userId) {
			return List.of(accessDto());
		}

		@Override
		public List<ApplicationAccessFacade.ApplicationAccessDto> replaceUserApplicationAccess(
			UUID userId,
			List<ApplicationAccessFacade.ApplicationAccessGrantCommand> grants,
			UUID grantedBy
		) {
			return List.of(accessDto());
		}

		@Override
		public ApplicationAccessFacade.ApplicationAccessDto grantUserApplicationAccess(
			UUID userId,
			UUID applicationId,
			String accessLevel,
			UUID grantedBy
		) {
			return accessDto();
		}

		@Override
		public void removeUserApplicationAccess(UUID userId, UUID applicationId) {
			// No-op for matcher tests.
		}

		@Override
		public boolean canViewApplication(UUID userId, UUID applicationId) {
			return true;
		}

		@Override
		public boolean canManageApplication(UUID userId, UUID applicationId) {
			return true;
		}

		@Override
		public ApplicationAccessFacade.ApiKeyCreationDto createApiKey(
			UUID applicationId,
			String name,
			Instant expiresAt,
			UUID createdBy
		) {
			return new ApplicationAccessFacade.ApiKeyCreationDto(
				UUID.randomUUID(),
				APPLICATION_ID,
				name,
				"lms_live_test",
				"lms_live_test.secret",
				"ACTIVE",
				expiresAt,
				NOW
			);
		}

		@Override
		public List<ApplicationAccessFacade.ApiKeyDto> findApiKeys(UUID applicationId) {
			return List.of();
		}

		@Override
		public ApplicationAccessFacade.ApiKeyCreationDto rotateApiKey(
			UUID applicationId,
			UUID apiKeyId,
			UUID rotatedBy
		) {
			return createApiKey(applicationId, "rotated", null, rotatedBy);
		}

		@Override
		public void revokeApiKey(UUID applicationId, UUID apiKeyId, UUID revokedBy) {
			// No-op for matcher tests.
		}

		@Override
		public ApplicationAccessFacade.ApiKeyVerificationDto verifyApplicationApiKey(String rawApiKey) {
			return new ApplicationAccessFacade.ApiKeyVerificationDto(
				true, APPLICATION_ID, "gateway", "Gateway", null
			);
		}

		private static ApplicationAccessFacade.ApplicationDto applicationDto() {
			return new ApplicationAccessFacade.ApplicationDto(
				APPLICATION_ID, "gateway", "Gateway", null, "ACTIVE", NOW, NOW
			);
		}

		private static ApplicationAccessFacade.ApplicationAccessDto accessDto() {
			return new ApplicationAccessFacade.ApplicationAccessDto(
				USER_ID,
				APPLICATION_ID,
				"gateway",
				"Gateway",
				"VIEW",
				USER_ID,
				NOW,
				NOW
			);
		}
	}
}
