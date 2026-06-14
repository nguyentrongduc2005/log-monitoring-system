package com.vdt.log_monitoring.modules.identity.internal.apikey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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

import com.vdt.log_monitoring.modules.identity.internal.application.ApplicationEntity;
import com.vdt.log_monitoring.modules.identity.internal.application.ApplicationRepository;
import com.vdt.log_monitoring.modules.identity.internal.application.ApplicationStatus;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceCacheTest {

	private static final UUID API_KEY_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");
	private static final UUID APPLICATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
	private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final String RAW_KEY = "test_live_fixed.secret";

	@Mock
	private ApplicationRepository applicationRepository;

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

	private ApiKeyService service;

	@BeforeEach
	void setUp() {
		lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
		service = new ApiKeyService(
			applicationRepository,
			apiKeyRepository,
			passwordEncoder,
			redisTemplate,
			apiKeyProperties()
		);
	}

	@Test
	void secondVerificationUsesCacheWithoutDatabaseOrPasswordCheck() {
		ApplicationApiKeyEntity apiKey = apiKey(Instant.now().plusSeconds(600));
		when(apiKeyRepository.findByKeyPrefix("test_live_fixed")).thenReturn(Optional.of(apiKey));
		when(passwordEncoder.matches(RAW_KEY, "hash")).thenReturn(true);
		when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(application()));

		ApiKeyService.ApiKeyVerification first = service.verifyApplicationApiKey(RAW_KEY);

		ArgumentCaptor<String> cacheKey = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> cacheValue = ArgumentCaptor.forClass(String.class);
		verify(valueOperations).set(
			cacheKey.capture(),
			cacheValue.capture(),
			eq(300L),
			eq(TimeUnit.SECONDS)
		);
		when(valueOperations.get(cacheKey.getValue())).thenReturn(cacheValue.getValue());

		ApiKeyService.ApiKeyVerification second = service.verifyApplicationApiKey(RAW_KEY);

		assertThat(first).isEqualTo(second);
		assertThat(cacheKey.getValue()).startsWith("verify:").doesNotContain(RAW_KEY);
		verify(apiKeyRepository, times(1)).findByKeyPrefix("test_live_fixed");
		verify(passwordEncoder, times(1)).matches(RAW_KEY, "hash");
		verify(applicationRepository, times(1)).findById(APPLICATION_ID);
	}

	@Test
	void cacheTtlDoesNotOutliveApiKeyExpiration() {
		ApplicationApiKeyEntity apiKey = apiKey(Instant.now().plusSeconds(30));
		when(apiKeyRepository.findByKeyPrefix("test_live_fixed")).thenReturn(Optional.of(apiKey));
		when(passwordEncoder.matches(RAW_KEY, "hash")).thenReturn(true);
		when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(application()));

		service.verifyApplicationApiKey(RAW_KEY);

		ArgumentCaptor<Long> ttl = ArgumentCaptor.forClass(Long.class);
		verify(valueOperations).set(anyString(), anyString(), ttl.capture(), eq(TimeUnit.SECONDS));
		assertThat(ttl.getValue()).isBetween(1L, 30L);
	}

	@Test
	void revokeEvictsAllCachedFingerprintsForApiKey() {
		ApplicationApiKeyEntity apiKey = apiKey(null);
		when(apiKeyRepository.findById(API_KEY_ID)).thenReturn(Optional.of(apiKey));
		when(setOperations.members("verify-index:" + API_KEY_ID))
			.thenReturn(Set.of("verify:fingerprint-a", "verify:fingerprint-b"));

		service.revokeApiKey(APPLICATION_ID, API_KEY_ID, ADMIN_ID);

		assertThat(apiKey.getStatus()).isEqualTo(ApiKeyStatus.REVOKED);
		verify(redisTemplate).delete(Set.of("verify:fingerprint-a", "verify:fingerprint-b"));
		verify(redisTemplate).delete("verify-index:" + API_KEY_ID);
		verify(apiKeyRepository, never()).save(apiKey);
	}

	private ApplicationApiKeyEntity apiKey(Instant expiresAt) {
		return ApplicationApiKeyEntity.restore(
			API_KEY_ID,
			APPLICATION_ID,
			"ingest",
			"test_live_fixed",
			"hash",
			ApiKeyStatus.ACTIVE,
			expiresAt,
			null,
			ADMIN_ID,
			Instant.parse("2026-06-10T00:00:00Z"),
			null
		);
	}

	private ApplicationEntity application() {
		return ApplicationEntity.restore(
			APPLICATION_ID,
			"gateway",
			"Gateway",
			null,
			ApplicationStatus.ACTIVE,
			ADMIN_ID,
			Instant.parse("2026-06-10T00:00:00Z"),
			Instant.parse("2026-06-10T00:00:00Z")
		);
	}

	private ApiKeyProperties apiKeyProperties() {
		ApiKeyProperties properties = new ApiKeyProperties();
		properties.setKeyPrefix("test_live_");
		properties.setPrefixCharacters("abc123");
		properties.setRandomPrefixLength(8);
		properties.setSecretLengthBytes(24);
		properties.getVerificationCache().setPrefix("verify:");
		properties.getVerificationCache().setIndexPrefix("verify-index:");
		properties.getVerificationCache().setTtlSeconds(300);
		return properties;
	}
}
