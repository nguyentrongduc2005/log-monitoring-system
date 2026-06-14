package com.vdt.log_monitoring.modules.identity.internal.apikey;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vdt.log_monitoring.modules.identity.api.IdentityException;
import com.vdt.log_monitoring.modules.identity.internal.application.ApplicationEntity;
import com.vdt.log_monitoring.modules.identity.internal.application.ApplicationRepository;
import com.vdt.log_monitoring.modules.identity.internal.application.ApplicationStatus;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

	private static final SecureRandom RANDOM = new SecureRandom();
	private static final String CACHE_VALUE_SEPARATOR = ":";

	private final ApplicationRepository applicationRepository;
	private final ApplicationApiKeyRepository apiKeyRepository;
	private final PasswordEncoder passwordEncoder;
	private final StringRedisTemplate redisTemplate;
	private final ApiKeyProperties properties;

	@Transactional
	public ApiKeyCreation createApiKey(UUID applicationId, String name, Instant expiresAt, UUID createdBy) {
		validateActiveApplication(applicationId);
		GeneratedApiKey generated = generateRawApiKey();
		ApplicationApiKeyEntity apiKey = ApplicationApiKeyEntity.create(
			applicationId,
			name,
			generated.prefix(),
			passwordEncoder.encode(generated.rawKey()),
			expiresAt,
			createdBy
		);

		ApplicationApiKeyEntity saved = apiKeyRepository.save(apiKey);
		return new ApiKeyCreation(saved, generated.rawKey());
	}

	@Transactional(readOnly = true)
	public List<ApplicationApiKeyEntity> findApiKeys(UUID applicationId) {
		validateApplicationExists(applicationId);
		return apiKeyRepository.findByApplicationId(applicationId);
	}

	@Transactional
	public ApiKeyCreation rotateApiKey(UUID applicationId, UUID apiKeyId, UUID rotatedBy) {
		validateActiveApplication(applicationId);
		ApplicationApiKeyEntity oldKey = getApiKey(applicationId, apiKeyId);
		oldKey.revoke();
		evictVerificationCache(oldKey.getId());

		return createApiKey(applicationId, oldKey.getName(), oldKey.getExpiresAt(), rotatedBy);
	}

	@Transactional
	public void revokeApiKey(UUID applicationId, UUID apiKeyId, UUID revokedBy) {
		ApplicationApiKeyEntity apiKey = getApiKey(applicationId, apiKeyId);
		apiKey.revoke();
		evictVerificationCache(apiKey.getId());
	}

	@Transactional(readOnly = true)
	public ApiKeyVerification verifyApplicationApiKey(String rawApiKey) {
		String keyPrefix = extractPrefix(rawApiKey);
		if (keyPrefix == null) {
			return invalid(IdentityException.ErrorCode.INVALID_API_KEY);
		}

		String cacheKey = verificationCacheKey(rawApiKey);
		ApiKeyVerification cached = readCachedVerification(cacheKey);
		if (cached != null) {
			return cached;
		}

		ApplicationApiKeyEntity apiKey = apiKeyRepository.findByKeyPrefix(keyPrefix).orElse(null);
		if (apiKey == null || !passwordEncoder.matches(rawApiKey, apiKey.getKeyHash())) {
			return invalid(IdentityException.ErrorCode.INVALID_API_KEY);
		}
		if (apiKey.getStatus() == ApiKeyStatus.REVOKED) {
			return invalid(IdentityException.ErrorCode.API_KEY_REVOKED);
		}
		if (apiKey.getStatus() == ApiKeyStatus.EXPIRED || !apiKey.isUsable(Instant.now())) {
			return invalid(IdentityException.ErrorCode.API_KEY_EXPIRED);
		}

		ApplicationEntity application = applicationRepository.findById(apiKey.getApplicationId()).orElse(null);
		if (application == null || application.getStatus() != ApplicationStatus.ACTIVE) {
			return invalid(IdentityException.ErrorCode.APPLICATION_INACTIVE);
		}

		ApiKeyVerification verification = new ApiKeyVerification(
			true,
			application.getId(),
			application.getName(),
			application.getDisplayName(),
			null
		);
		cacheVerification(cacheKey, apiKey, verification);
		return verification;
	}

	private ApplicationApiKeyEntity getApiKey(UUID applicationId, UUID apiKeyId) {
		ApplicationApiKeyEntity apiKey = apiKeyRepository.findById(apiKeyId)
			.orElseThrow(() -> new IdentityException(
				IdentityException.ErrorCode.API_KEY_NOT_FOUND,
				"API key not found"
			));
		if (!apiKey.getApplicationId().equals(applicationId)) {
			throw new IdentityException(
				IdentityException.ErrorCode.API_KEY_NOT_FOUND,
				"API key not found"
			);
		}
		return apiKey;
	}

	private ApplicationEntity validateApplicationExists(UUID applicationId) {
		return applicationRepository.findById(applicationId)
			.orElseThrow(() -> new IdentityException(
				IdentityException.ErrorCode.APPLICATION_NOT_FOUND,
				"Application not found"
			));
	}

	private void validateActiveApplication(UUID applicationId) {
		ApplicationEntity application = validateApplicationExists(applicationId);
		if (application.getStatus() != ApplicationStatus.ACTIVE) {
			throw new IdentityException(
				IdentityException.ErrorCode.APPLICATION_INACTIVE,
				"Application is inactive"
			);
		}
	}

	private GeneratedApiKey generateRawApiKey() {
		String prefix = properties.getKeyPrefix()
			+ randomPrefixSegment(properties.getRandomPrefixLength());
		byte[] secretBytes = new byte[properties.getSecretLengthBytes()];
		RANDOM.nextBytes(secretBytes);
		String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);
		return new GeneratedApiKey(prefix, prefix + "." + secret);
	}

	private String randomPrefixSegment(int length) {
		char[] prefixCharacters = properties.getPrefixCharacters().toCharArray();
		StringBuilder segment = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			segment.append(prefixCharacters[RANDOM.nextInt(prefixCharacters.length)]);
		}
		return segment.toString();
	}

	private ApiKeyVerification readCachedVerification(String cacheKey) {
		String cachedValue = redisTemplate.opsForValue().get(cacheKey);
		if (cachedValue == null) {
			return null;
		}

		String[] fields = cachedValue.split(CACHE_VALUE_SEPARATOR, 3);
		if (fields.length != 3) {
			redisTemplate.delete(cacheKey);
			return null;
		}

		try {
			return new ApiKeyVerification(
				true,
				UUID.fromString(fields[0]),
				decodeCacheField(fields[1]),
				decodeCacheField(fields[2]),
				null
			);
		} catch (IllegalArgumentException exception) {
			redisTemplate.delete(cacheKey);
			return null;
		}
	}

	private void cacheVerification(
		String cacheKey,
		ApplicationApiKeyEntity apiKey,
		ApiKeyVerification verification
	) {
		long ttlSeconds = effectiveCacheTtlSeconds(apiKey.getExpiresAt());
		if (ttlSeconds <= 0) {
			return;
		}

		String cacheValue = verification.applicationId()
			+ CACHE_VALUE_SEPARATOR
			+ encodeCacheField(verification.applicationName())
			+ CACHE_VALUE_SEPARATOR
			+ encodeCacheField(verification.applicationDisplayName());
		redisTemplate.opsForValue().set(cacheKey, cacheValue, ttlSeconds, TimeUnit.SECONDS);

		String indexKey = properties.getVerificationCache().getIndexPrefix() + apiKey.getId();
		redisTemplate.opsForSet().add(indexKey, cacheKey);
		redisTemplate.expire(indexKey, ttlSeconds, TimeUnit.SECONDS);
	}

	private void evictVerificationCache(UUID apiKeyId) {
		String indexKey = properties.getVerificationCache().getIndexPrefix() + apiKeyId;
		Set<String> cacheKeys = redisTemplate.opsForSet().members(indexKey);
		if (cacheKeys != null && !cacheKeys.isEmpty()) {
			redisTemplate.delete(cacheKeys);
		}
		redisTemplate.delete(indexKey);
	}

	private long effectiveCacheTtlSeconds(Instant expiresAt) {
		if (expiresAt == null) {
			return properties.getVerificationCache().getTtlSeconds();
		}
		long secondsUntilExpiry = expiresAt.getEpochSecond() - Instant.now().getEpochSecond();
		return Math.min(properties.getVerificationCache().getTtlSeconds(), secondsUntilExpiry);
	}

	private String verificationCacheKey(String rawApiKey) {
		return properties.getVerificationCache().getPrefix() + sha256(rawApiKey);
	}

	private String sha256(String value) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256")
				.digest(value.getBytes(StandardCharsets.UTF_8));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is not available", exception);
		}
	}

	private String encodeCacheField(String value) {
		return Base64.getUrlEncoder().withoutPadding()
			.encodeToString(value.getBytes(StandardCharsets.UTF_8));
	}

	private String decodeCacheField(String value) {
		return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
	}

	private String extractPrefix(String rawApiKey) {
		if (rawApiKey == null || rawApiKey.isBlank()) {
			return null;
		}
		int separator = rawApiKey.indexOf('.');
		if (separator <= 0 || separator == rawApiKey.length() - 1) {
			return null;
		}
		return rawApiKey.substring(0, separator);
	}

	private ApiKeyVerification invalid(IdentityException.ErrorCode errorCode) {
		return new ApiKeyVerification(
			false,
			null,
			null,
			null,
			errorCode.name()
		);
	}

	public record ApiKeyCreation(ApplicationApiKeyEntity apiKey, String rawApiKey) {}

	public record ApiKeyVerification(
		boolean valid,
		UUID applicationId,
		String applicationName,
		String applicationDisplayName,
		String failureReason
	) {}

	private record GeneratedApiKey(String prefix, String rawKey) {}
}
