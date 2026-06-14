package com.vdt.log_monitoring.modules.identity.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.vdt.log_monitoring.modules.identity.api.ApplicationAccessFacade;
import com.vdt.log_monitoring.modules.identity.api.IdentityException;
import com.vdt.log_monitoring.modules.identity.internal.access.ApplicationAccessLevel;
import com.vdt.log_monitoring.modules.identity.internal.access.ApplicationAccessService;
import com.vdt.log_monitoring.modules.identity.internal.access.UserApplicationAccessEntity;
import com.vdt.log_monitoring.modules.identity.internal.access.UserApplicationAccessId;
import com.vdt.log_monitoring.modules.identity.internal.access.UserApplicationAccessRepository;
import com.vdt.log_monitoring.modules.identity.internal.apikey.ApiKeyService;
import com.vdt.log_monitoring.modules.identity.internal.apikey.ApiKeyStatus;
import com.vdt.log_monitoring.modules.identity.internal.apikey.ApiKeyProperties;
import com.vdt.log_monitoring.modules.identity.internal.apikey.ApplicationApiKeyEntity;
import com.vdt.log_monitoring.modules.identity.internal.apikey.ApplicationApiKeyRepository;
import com.vdt.log_monitoring.modules.identity.internal.application.ApplicationEntity;
import com.vdt.log_monitoring.modules.identity.internal.application.ApplicationRepository;
import com.vdt.log_monitoring.modules.identity.internal.application.ApplicationService;
import com.vdt.log_monitoring.modules.identity.internal.application.ApplicationStatus;
import com.vdt.log_monitoring.modules.identity.internal.user.UserEntity;
import com.vdt.log_monitoring.modules.identity.internal.user.UserRepository;
import com.vdt.log_monitoring.modules.identity.internal.user.UserRole;
import com.vdt.log_monitoring.modules.identity.internal.user.UserStatus;

@ExtendWith(MockitoExtension.class)
class ApplicationAccessFacadeImplTest {

	private static final Instant NOW = Instant.parse("2026-06-09T10:00:00Z");
	private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID ENGINEER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
	private static final UUID APP_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
	private static final UUID APP_ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000102");

	@Mock
	private ApplicationRepository applicationRepository;

	@Mock
	private UserApplicationAccessRepository accessRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private ApplicationApiKeyRepository apiKeyRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@Mock
	private SetOperations<String, String> setOperations;

	private ApplicationAccessFacade facade;

	@BeforeEach
	void setUp() {
		lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
		ApplicationService applicationService = new ApplicationService(applicationRepository, accessRepository);
		ApplicationAccessService accessService = new ApplicationAccessService(
			userRepository,
			applicationRepository,
			accessRepository
		);
		ApiKeyService apiKeyService = new ApiKeyService(
			applicationRepository,
			apiKeyRepository,
			passwordEncoder,
			redisTemplate,
			apiKeyProperties()
		);
		facade = new ApplicationAccessFacadeImpl(applicationService, accessService, apiKeyService);
	}

	@Test
	void createApplicationNormalizesNameAndRejectsDuplicates() {
		when(applicationRepository.existsByNameIgnoreCase("service-api")).thenReturn(false);
		when(applicationRepository.save(any(ApplicationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ApplicationAccessFacade.ApplicationDto created = facade.createApplication(
			new ApplicationAccessFacade.CreateApplicationCommand(
				" service-api ",
				" Service API ",
				" Internal logs ",
				ADMIN_ID
			)
		);

		assertThat(created.name()).isEqualTo("service-api");
		assertThat(created.displayName()).isEqualTo("Service API");
		assertThat(created.description()).isEqualTo("Internal logs");

		when(applicationRepository.existsByNameIgnoreCase("service-api")).thenReturn(true);

		assertThatThrownBy(() -> facade.createApplication(
			new ApplicationAccessFacade.CreateApplicationCommand(" service-api ", "Service API", null, ADMIN_ID)
		))
			.isInstanceOf(IdentityException.class)
			.extracting("errorCode")
			.isEqualTo(IdentityException.ErrorCode.APPLICATION_NAME_ALREADY_EXISTS);
	}

	@Test
	void findVisibleApplicationsReturnsAllActiveApplicationsForAdmin() {
		when(applicationRepository.findByStatus(ApplicationStatus.ACTIVE))
			.thenReturn(List.of(applicationEntity(APP_ID, "gateway", ApplicationStatus.ACTIVE)));

		List<ApplicationAccessFacade.ApplicationDto> applications =
			facade.findVisibleApplications(ADMIN_ID, "ADMIN");

		assertThat(applications)
			.extracting(ApplicationAccessFacade.ApplicationDto::name)
			.containsExactly("gateway");
	}

	@Test
	void findVisibleApplicationsReturnsActiveGrantedApplicationsForEngineer() {
		when(applicationRepository.findVisibleByUserIdAndStatus(ENGINEER_ID, ApplicationStatus.ACTIVE))
			.thenReturn(List.of(applicationEntity(APP_ID, "gateway", ApplicationStatus.ACTIVE)));

		List<ApplicationAccessFacade.ApplicationDto> applications =
			facade.findVisibleApplications(ENGINEER_ID, "ENGINEER");

		assertThat(applications)
			.extracting(ApplicationAccessFacade.ApplicationDto::name)
			.containsExactly("gateway");
		verify(applicationRepository).findVisibleByUserIdAndStatus(ENGINEER_ID, ApplicationStatus.ACTIVE);
		verify(applicationRepository, never()).findById(any());
	}

	@Test
	void replaceUserApplicationAccessRejectsDuplicateApplicationIds() {
		assertThatThrownBy(() -> facade.replaceUserApplicationAccess(
			ENGINEER_ID,
			List.of(
				new ApplicationAccessFacade.ApplicationAccessGrantCommand(APP_ID, "VIEW"),
				new ApplicationAccessFacade.ApplicationAccessGrantCommand(APP_ID, "MANAGE")
			),
			ADMIN_ID
		))
			.isInstanceOf(IdentityException.class)
			.extracting("errorCode")
			.isEqualTo(IdentityException.ErrorCode.INVALID_APPLICATION_ACCESS_GRANT);
	}

	@Test
	void replaceUserApplicationAccessWithEmptyListRemovesAllExplicitGrants() {
		when(userRepository.findById(ENGINEER_ID)).thenReturn(Optional.of(userEntity(ENGINEER_ID, UserRole.ENGINEER)));
		when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(userEntity(ADMIN_ID, UserRole.ADMIN)));

		List<ApplicationAccessFacade.ApplicationAccessDto> grants =
			facade.replaceUserApplicationAccess(ENGINEER_ID, List.of(), ADMIN_ID);

		assertThat(grants).isEmpty();
		verify(accessRepository).deleteByIdUserId(ENGINEER_ID);
		verify(accessRepository, never()).save(any(UserApplicationAccessEntity.class));
	}

	@Test
	void findUserApplicationAccessLoadsApplicationDetailsInOneBatch() {
		when(userRepository.findById(ENGINEER_ID))
			.thenReturn(Optional.of(userEntity(ENGINEER_ID, UserRole.ENGINEER)));
		when(accessRepository.findByIdUserId(ENGINEER_ID))
			.thenReturn(List.of(
				accessEntity(ENGINEER_ID, APP_ID, ApplicationAccessLevel.VIEW),
				accessEntity(ENGINEER_ID, APP_ID_2, ApplicationAccessLevel.MANAGE)
			));
		when(applicationRepository.findAllById(any()))
			.thenReturn(List.of(
				applicationEntity(APP_ID, "gateway", ApplicationStatus.ACTIVE),
				applicationEntity(APP_ID_2, "billing", ApplicationStatus.ACTIVE)
			));

		List<ApplicationAccessFacade.ApplicationAccessDto> grants =
			facade.findUserApplicationAccess(ENGINEER_ID);

		assertThat(grants)
			.extracting(ApplicationAccessFacade.ApplicationAccessDto::applicationName)
			.containsExactly("gateway", "billing");
		verify(applicationRepository, times(1)).findAllById(any());
		verify(applicationRepository, never()).findById(any());
	}

	@Test
	void grantUserApplicationAccessCreatesAndUpdatesRows() {
		when(userRepository.findById(ENGINEER_ID)).thenReturn(Optional.of(userEntity(ENGINEER_ID, UserRole.ENGINEER)));
		when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(userEntity(ADMIN_ID, UserRole.ADMIN)));
		when(applicationRepository.findById(APP_ID))
			.thenReturn(Optional.of(applicationEntity(APP_ID, "gateway", ApplicationStatus.ACTIVE)));
		when(accessRepository.save(any(UserApplicationAccessEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		facade.grantUserApplicationAccess(ENGINEER_ID, APP_ID, "VIEW", ADMIN_ID);

		ArgumentCaptor<UserApplicationAccessEntity> createCaptor =
			ArgumentCaptor.forClass(UserApplicationAccessEntity.class);
		verify(accessRepository).save(createCaptor.capture());
		assertThat(createCaptor.getValue().getAccessLevel()).isEqualTo(ApplicationAccessLevel.VIEW);

		when(accessRepository.findById(new UserApplicationAccessId(ENGINEER_ID, APP_ID)))
			.thenReturn(Optional.of(accessEntity(ENGINEER_ID, APP_ID, ApplicationAccessLevel.VIEW)));

		ApplicationAccessFacade.ApplicationAccessDto updated =
			facade.grantUserApplicationAccess(ENGINEER_ID, APP_ID, "MANAGE", ADMIN_ID);

		assertThat(updated.accessLevel()).isEqualTo("MANAGE");
	}

	@Test
	void canViewApplicationAllowsAdminViewAndManageGrants() {
		when(applicationRepository.findById(APP_ID))
			.thenReturn(Optional.of(applicationEntity(APP_ID, "gateway", ApplicationStatus.ACTIVE)));
		when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(userEntity(ADMIN_ID, UserRole.ADMIN)));

		assertThat(facade.canViewApplication(ADMIN_ID, APP_ID)).isTrue();

		when(userRepository.findById(ENGINEER_ID)).thenReturn(Optional.of(userEntity(ENGINEER_ID, UserRole.ENGINEER)));
		when(accessRepository.findById(new UserApplicationAccessId(ENGINEER_ID, APP_ID)))
			.thenReturn(Optional.of(accessEntity(ENGINEER_ID, APP_ID, ApplicationAccessLevel.VIEW)))
			.thenReturn(Optional.of(accessEntity(ENGINEER_ID, APP_ID, ApplicationAccessLevel.MANAGE)));

		assertThat(facade.canViewApplication(ENGINEER_ID, APP_ID)).isTrue();
		assertThat(facade.canViewApplication(ENGINEER_ID, APP_ID)).isTrue();
	}

	@Test
	void canManageApplicationAllowsAdminAndExplicitManageOnly() {
		when(applicationRepository.findById(APP_ID))
			.thenReturn(Optional.of(applicationEntity(APP_ID, "gateway", ApplicationStatus.ACTIVE)));
		when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(userEntity(ADMIN_ID, UserRole.ADMIN)));

		assertThat(facade.canManageApplication(ADMIN_ID, APP_ID)).isTrue();

		when(userRepository.findById(ENGINEER_ID)).thenReturn(Optional.of(userEntity(ENGINEER_ID, UserRole.ENGINEER)));
		when(accessRepository.findById(new UserApplicationAccessId(ENGINEER_ID, APP_ID)))
			.thenReturn(Optional.of(accessEntity(ENGINEER_ID, APP_ID, ApplicationAccessLevel.VIEW)))
			.thenReturn(Optional.of(accessEntity(ENGINEER_ID, APP_ID, ApplicationAccessLevel.MANAGE)));

		assertThat(facade.canManageApplication(ENGINEER_ID, APP_ID)).isFalse();
		assertThat(facade.canManageApplication(ENGINEER_ID, APP_ID)).isTrue();
	}

	@Test
	void createApiKeyReturnsRawKeyOnceAndStoresOnlyHashAndPrefix() {
		when(applicationRepository.findById(APP_ID))
			.thenReturn(Optional.of(applicationEntity(APP_ID, "gateway", ApplicationStatus.ACTIVE)));
		when(passwordEncoder.encode(anyString())).thenReturn("encoded-api-key");
		when(apiKeyRepository.save(any(ApplicationApiKeyEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ApplicationAccessFacade.ApiKeyCreationDto created =
			facade.createApiKey(APP_ID, " Ingest Key ", Instant.parse("2099-01-01T00:00:00Z"), ADMIN_ID);

		ArgumentCaptor<String> rawKeyCaptor = ArgumentCaptor.forClass(String.class);
		verify(passwordEncoder).encode(rawKeyCaptor.capture());
		ArgumentCaptor<ApplicationApiKeyEntity> entityCaptor =
			ArgumentCaptor.forClass(ApplicationApiKeyEntity.class);
		verify(apiKeyRepository).save(entityCaptor.capture());

		ApplicationApiKeyEntity stored = entityCaptor.getValue();
		assertThat(created.rawApiKey()).isEqualTo(rawKeyCaptor.getValue());
		assertThat(created.keyPrefix()).startsWith("lms_live_");
		assertThat(created.rawApiKey()).startsWith(created.keyPrefix() + ".");
		assertThat(stored.getKeyPrefix()).isEqualTo(created.keyPrefix());
		assertThat(stored.getKeyHash()).isEqualTo("encoded-api-key");
		assertThat(stored.getKeyHash()).isNotEqualTo(created.rawApiKey());
	}

	@Test
	void oneApplicationCanHaveMultipleActiveApiKeys() {
		when(applicationRepository.findById(APP_ID))
			.thenReturn(Optional.of(applicationEntity(APP_ID, "gateway", ApplicationStatus.ACTIVE)));
		when(apiKeyRepository.findByApplicationId(APP_ID))
			.thenReturn(List.of(
				apiKeyEntity(UUID.fromString("00000000-0000-0000-0000-000000000201"), APP_ID, "key-a", "lms_live_a", "hash-a", ApiKeyStatus.ACTIVE, null),
				apiKeyEntity(UUID.fromString("00000000-0000-0000-0000-000000000202"), APP_ID, "key-b", "lms_live_b", "hash-b", ApiKeyStatus.ACTIVE, null)
			));

		List<ApplicationAccessFacade.ApiKeyDto> keys = facade.findApiKeys(APP_ID);

		assertThat(keys)
			.extracting(ApplicationAccessFacade.ApiKeyDto::keyPrefix)
			.containsExactly("lms_live_a", "lms_live_b");
		assertThat(keys)
			.extracting(ApplicationAccessFacade.ApiKeyDto::status)
			.containsExactly("ACTIVE", "ACTIVE");
	}

	@Test
	void rotateApiKeyRevokesOldKeyAndCreatesNewRawKeyRecord() {
		UUID oldKeyId = UUID.fromString("00000000-0000-0000-0000-000000000201");
		when(applicationRepository.findById(APP_ID))
			.thenReturn(Optional.of(applicationEntity(APP_ID, "gateway", ApplicationStatus.ACTIVE)));
		when(apiKeyRepository.findById(oldKeyId))
			.thenReturn(Optional.of(apiKeyEntity(oldKeyId, APP_ID, "old-key", "lms_live_old", "old-hash", ApiKeyStatus.ACTIVE, null)));
		when(passwordEncoder.encode(anyString())).thenReturn("new-hash");
		when(apiKeyRepository.save(any(ApplicationApiKeyEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ApplicationAccessFacade.ApiKeyCreationDto rotated = facade.rotateApiKey(APP_ID, oldKeyId, ADMIN_ID);

		ArgumentCaptor<ApplicationApiKeyEntity> entityCaptor =
			ArgumentCaptor.forClass(ApplicationApiKeyEntity.class);
		verify(apiKeyRepository).save(entityCaptor.capture());

		assertThat(entityCaptor.getValue().getStatus()).isEqualTo(ApiKeyStatus.ACTIVE);
		assertThat(rotated.rawApiKey()).startsWith(rotated.keyPrefix() + ".");
		assertThat(rotated.keyPrefix()).isNotEqualTo("lms_live_old");
	}

	@Test
	void revokeApiKeyMarksKeyRevoked() {
		UUID keyId = UUID.fromString("00000000-0000-0000-0000-000000000201");
		ApplicationApiKeyEntity apiKey =
			apiKeyEntity(keyId, APP_ID, "old-key", "lms_live_old", "old-hash", ApiKeyStatus.ACTIVE, null);
		when(apiKeyRepository.findById(keyId)).thenReturn(Optional.of(apiKey));

		facade.revokeApiKey(APP_ID, keyId, ADMIN_ID);

		assertThat(apiKey.getStatus()).isEqualTo(ApiKeyStatus.REVOKED);
		assertThat(apiKey.getRevokedAt()).isNotNull();
	}

	@Test
	void verifyApplicationApiKeyReturnsValidApplicationDetails() {
		String rawKey = "lms_live_valid.secret";
		when(apiKeyRepository.findByKeyPrefix("lms_live_valid"))
			.thenReturn(Optional.of(apiKeyEntity(
				UUID.fromString("00000000-0000-0000-0000-000000000201"),
				APP_ID,
				"ingest",
				"lms_live_valid",
				"hash",
				ApiKeyStatus.ACTIVE,
				Instant.parse("2099-01-01T00:00:00Z")
			)));
		when(passwordEncoder.matches(rawKey, "hash")).thenReturn(true);
		when(applicationRepository.findById(APP_ID))
			.thenReturn(Optional.of(applicationEntity(APP_ID, "gateway", ApplicationStatus.ACTIVE)));

		ApplicationAccessFacade.ApiKeyVerificationDto result = facade.verifyApplicationApiKey(rawKey);

		assertThat(result.valid()).isTrue();
		assertThat(result.applicationId()).isEqualTo(APP_ID);
		assertThat(result.applicationName()).isEqualTo("gateway");
		assertThat(result.failureReason()).isNull();
	}

	@Test
	void verifyApplicationApiKeyReturnsFailureReasonsForExpectedInvalidStates() {
		assertThat(facade.verifyApplicationApiKey("missing-dot").failureReason())
			.isEqualTo("INVALID_API_KEY");

		when(apiKeyRepository.findByKeyPrefix("lms_live_revoked"))
			.thenReturn(Optional.of(apiKeyEntity(
				UUID.fromString("00000000-0000-0000-0000-000000000201"),
				APP_ID,
				"revoked",
				"lms_live_revoked",
				"hash",
				ApiKeyStatus.REVOKED,
				null
			)));
		when(passwordEncoder.matches("lms_live_revoked.secret", "hash")).thenReturn(true);
		assertThat(facade.verifyApplicationApiKey("lms_live_revoked.secret").failureReason())
			.isEqualTo("API_KEY_REVOKED");

		when(apiKeyRepository.findByKeyPrefix("lms_live_expired"))
			.thenReturn(Optional.of(apiKeyEntity(
				UUID.fromString("00000000-0000-0000-0000-000000000202"),
				APP_ID,
				"expired",
				"lms_live_expired",
				"hash",
				ApiKeyStatus.ACTIVE,
				Instant.parse("2020-01-01T00:00:00Z")
			)));
		when(passwordEncoder.matches("lms_live_expired.secret", "hash")).thenReturn(true);
		assertThat(facade.verifyApplicationApiKey("lms_live_expired.secret").failureReason())
			.isEqualTo("API_KEY_EXPIRED");

		when(apiKeyRepository.findByKeyPrefix("lms_live_inactive"))
			.thenReturn(Optional.of(apiKeyEntity(
				UUID.fromString("00000000-0000-0000-0000-000000000203"),
				APP_ID,
				"inactive-app",
				"lms_live_inactive",
				"hash",
				ApiKeyStatus.ACTIVE,
				Instant.parse("2099-01-01T00:00:00Z")
			)));
		when(passwordEncoder.matches("lms_live_inactive.secret", "hash")).thenReturn(true);
		when(applicationRepository.findById(APP_ID))
			.thenReturn(Optional.of(applicationEntity(APP_ID, "gateway", ApplicationStatus.INACTIVE)));
		assertThat(facade.verifyApplicationApiKey("lms_live_inactive.secret").failureReason())
			.isEqualTo("APPLICATION_INACTIVE");

		when(apiKeyRepository.findByKeyPrefix("lms_live_bad_hash"))
			.thenReturn(Optional.of(apiKeyEntity(
				UUID.fromString("00000000-0000-0000-0000-000000000204"),
				APP_ID,
				"bad-hash",
				"lms_live_bad_hash",
				"hash",
				ApiKeyStatus.ACTIVE,
				Instant.parse("2099-01-01T00:00:00Z")
			)));
		when(passwordEncoder.matches("lms_live_bad_hash.secret", "hash")).thenReturn(false);
		assertThat(facade.verifyApplicationApiKey("lms_live_bad_hash.secret").failureReason())
			.isEqualTo("INVALID_API_KEY");
	}

	private static ApplicationEntity applicationEntity(UUID id, String name, ApplicationStatus status) {
		return ApplicationEntity.restore(
			id,
			name,
			name.toUpperCase(),
			null,
			status,
			ADMIN_ID,
			NOW,
			NOW
		);
	}

	private static UserApplicationAccessEntity accessEntity(
		UUID userId,
		UUID applicationId,
		ApplicationAccessLevel accessLevel
	) {
		return UserApplicationAccessEntity.restore(
			userId,
			applicationId,
			accessLevel,
			ADMIN_ID,
			NOW,
			NOW
		);
	}

	private static UserEntity userEntity(UUID id, UserRole role) {
		return UserEntity.restore(
			id,
			role.name().toLowerCase() + "@example.com",
			"password-hash",
			role.name(),
			role,
			UserStatus.ACTIVE,
			null,
			NOW,
			NOW
		);
	}

	private static ApplicationApiKeyEntity apiKeyEntity(
		UUID id,
		UUID applicationId,
		String name,
		String keyPrefix,
		String keyHash,
		ApiKeyStatus status,
		Instant expiresAt
	) {
		return ApplicationApiKeyEntity.restore(
			id,
			applicationId,
			name,
			keyPrefix,
			keyHash,
			status,
			expiresAt,
			null,
			ADMIN_ID,
			NOW,
			status == ApiKeyStatus.REVOKED ? NOW : null
		);
	}

	private static ApiKeyProperties apiKeyProperties() {
		ApiKeyProperties properties = new ApiKeyProperties();
		properties.setKeyPrefix("lms_live_");
		properties.setPrefixCharacters("abcdefghijklmnopqrstuvwxyz0123456789");
		properties.setRandomPrefixLength(10);
		properties.setSecretLengthBytes(32);
		properties.getVerificationCache().setPrefix("identity:api_key:verification:");
		properties.getVerificationCache().setIndexPrefix("identity:api_key:verification_index:");
		properties.getVerificationCache().setTtlSeconds(300);
		return properties;
	}
}
