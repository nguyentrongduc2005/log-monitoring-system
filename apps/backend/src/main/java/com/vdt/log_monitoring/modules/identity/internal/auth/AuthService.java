package com.vdt.log_monitoring.modules.identity.internal.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vdt.log_monitoring.modules.identity.api.IdentityException;
import com.vdt.log_monitoring.modules.identity.internal.user.UserEntity;
import com.vdt.log_monitoring.modules.identity.internal.user.UserRepository;
import com.vdt.log_monitoring.modules.identity.internal.user.UserStatus;
import com.vdt.log_monitoring.shared.security.JwtTokenProvider;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final StringRedisTemplate redisTemplate;

	@Value("${app.security.jwt.refresh-token.prefix}")
	private String redisKeyPrefix;

	@Value("${app.security.jwt.refresh-token.user-prefix}")
	private String userRefreshTokenPrefix;

	@Value("${app.security.jwt.access-token.blacklist-prefix}")
	private String accessTokenBlacklistPrefix;

	@Value("${app.security.jwt.refresh-token.expiration-days}")
	private long refreshTokenTtlDays;

	@Transactional
	public TokenPair authenticate(String email, String rawPassword) {
		UserEntity entity = userRepository.findByEmailIgnoreCase(email.trim())
			.orElseThrow(() -> new IdentityException(IdentityException.ErrorCode.INVALID_CREDENTIALS, "Invalid email or password"));

		if (entity.getStatus() == UserStatus.DISABLED) {
			throw new IdentityException(IdentityException.ErrorCode.ACCOUNT_DISABLED, "Account is disabled");
		}
		if (entity.getStatus() == UserStatus.LOCKED) {
			throw new IdentityException(IdentityException.ErrorCode.ACCOUNT_LOCKED, "Account is locked");
		}
		if (entity.getStatus() == UserStatus.DELETED) {
			throw new IdentityException(IdentityException.ErrorCode.ACCOUNT_DISABLED, "Account is disabled");
		}

		if (!passwordEncoder.matches(rawPassword, entity.getPasswordHash())) {
			throw new IdentityException(IdentityException.ErrorCode.INVALID_CREDENTIALS, "Invalid email or password");
		}

		entity.recordSuccessfulLogin();

		String accessToken = jwtTokenProvider.generateToken(
			entity.getEmail(),
			entity.getRole().name(),
			entity.getDisplayName(),
			entity.getId().toString()
		);
		String refreshToken = createRefreshToken(entity.getId());

		return new TokenPair(accessToken, refreshToken, entity);
	}

	@Transactional
	public TokenPair refresh(String refreshTokenValue) {
		String redisKey = redisKeyPrefix + refreshTokenValue;
		String userIdStr = redisTemplate.opsForValue().get(redisKey);

		if (userIdStr == null) {
			throw new IdentityException(IdentityException.ErrorCode.INVALID_CREDENTIALS, "Invalid or expired refresh token");
		}

		UUID userId = UUID.fromString(userIdStr);
		assertCurrentRefreshToken(userId, refreshTokenValue, redisKey);

		UserEntity userEntity = userRepository.findById(userId)
			.orElseThrow(() -> new IdentityException(IdentityException.ErrorCode.USER_NOT_FOUND, "User not found"));
		if (userEntity.getStatus() == UserStatus.DELETED) {
			throw new IdentityException(IdentityException.ErrorCode.ACCOUNT_DISABLED, "Account is disabled");
		}

		// Rotate token: delete old one and create new one
		redisTemplate.delete(redisKey);
		String newRefreshToken = createRefreshToken(userId);

		String newAccessToken = jwtTokenProvider.generateToken(
			userEntity.getEmail(),
			userEntity.getRole().name(),
			userEntity.getDisplayName(),
			userEntity.getId().toString()
		);

		return new TokenPair(newAccessToken, newRefreshToken, userEntity);
	}

	@Transactional
	public void logout(String accessToken, String refreshTokenValue) {
		if (accessToken == null || accessToken.isBlank()) {
			throw new IdentityException(IdentityException.ErrorCode.INVALID_CREDENTIALS, "Missing access token");
		}
		if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
			throw new IdentityException(IdentityException.ErrorCode.INVALID_CREDENTIALS, "Missing refresh token");
		}

		revokeRefreshToken(refreshTokenValue);
		blacklistAccessToken(accessToken);
	}

	@Transactional
	public void changePassword(UUID id, String oldPassword, String newPassword) {
		UserEntity entity = userRepository.findById(id)
			.orElseThrow(() -> new IdentityException(IdentityException.ErrorCode.USER_NOT_FOUND, "User not found"));

		if (!passwordEncoder.matches(oldPassword, entity.getPasswordHash())) {
			throw new IdentityException(IdentityException.ErrorCode.INVALID_CREDENTIALS, "Incorrect current password");
		}

		entity.changePasswordHash(passwordEncoder.encode(newPassword));
	}

	private String createRefreshToken(UUID userId) {
		deleteCurrentRefreshToken(userId);

		String tokenValue = UUID.randomUUID().toString();
		String redisKey = redisKeyPrefix + tokenValue;
		String userTokenKey = userRefreshTokenPrefix + userId;

		// Save key in Redis with configured expiration time
		redisTemplate.opsForValue().set(redisKey, userId.toString(), refreshTokenTtlDays, TimeUnit.DAYS);
		redisTemplate.opsForValue().set(userTokenKey, tokenValue, refreshTokenTtlDays, TimeUnit.DAYS);
		return tokenValue;
	}

	private void assertCurrentRefreshToken(UUID userId, String refreshTokenValue, String redisKey) {
		String currentRefreshToken = redisTemplate.opsForValue().get(userRefreshTokenPrefix + userId);
		if (currentRefreshToken == null || !currentRefreshToken.equals(refreshTokenValue)) {
			redisTemplate.delete(redisKey);
			throw new IdentityException(IdentityException.ErrorCode.INVALID_CREDENTIALS, "Invalid or expired refresh token");
		}
	}

	private void deleteCurrentRefreshToken(UUID userId) {
		String userTokenKey = userRefreshTokenPrefix + userId;
		String currentRefreshToken = redisTemplate.opsForValue().get(userTokenKey);
		if (currentRefreshToken != null) {
			redisTemplate.delete(redisKeyPrefix + currentRefreshToken);
		}
	}

	private void revokeRefreshToken(String refreshTokenValue) {
		String redisKey = redisKeyPrefix + refreshTokenValue;
		String userIdStr = redisTemplate.opsForValue().get(redisKey);
		redisTemplate.delete(redisKey);

		if (userIdStr == null) {
			return;
		}

		String userTokenKey = userRefreshTokenPrefix + userIdStr;
		String currentRefreshToken = redisTemplate.opsForValue().get(userTokenKey);
		if (refreshTokenValue.equals(currentRefreshToken)) {
			redisTemplate.delete(userTokenKey);
		}
	}

	private void blacklistAccessToken(String accessToken) {
		jwtTokenProvider.validateTokenOrThrow(accessToken);
		long remainingTtlMillis = jwtTokenProvider.getRemainingValidityMillis(accessToken);
		if (remainingTtlMillis <= 0) {
			return;
		}
		redisTemplate.opsForValue().set(
			accessTokenBlacklistPrefix + hashToken(accessToken),
			"revoked",
			remainingTtlMillis,
			TimeUnit.MILLISECONDS
		);
	}

	private String hashToken(String token) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 is not available", ex);
		}
	}
}
