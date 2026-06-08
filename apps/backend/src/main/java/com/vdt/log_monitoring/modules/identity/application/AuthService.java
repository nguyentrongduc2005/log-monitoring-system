package com.vdt.log_monitoring.modules.identity.application;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vdt.log_monitoring.modules.identity.infrastructure.persistence.UserEntity;
import com.vdt.log_monitoring.modules.identity.infrastructure.persistence.UserRepository;
import com.vdt.log_monitoring.modules.identity.model.User;
import com.vdt.log_monitoring.modules.identity.model.UserStatus;
import com.vdt.log_monitoring.shared.security.JwtTokenProvider;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final StringRedisTemplate redisTemplate;

	@Value("${app.security.jwt.refresh-token.prefix:identity:refresh_token:}")
	private String redisKeyPrefix;

	@Value("${app.security.jwt.refresh-token.expiration-days:7}")
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

		if (!passwordEncoder.matches(rawPassword, entity.getPasswordHash())) {
			throw new IdentityException(IdentityException.ErrorCode.INVALID_CREDENTIALS, "Invalid email or password");
		}

		User user = entity.toModel();
		user.recordSuccessfulLogin(Instant.now());
		userRepository.save(UserEntity.fromModel(user));

		String accessToken = jwtTokenProvider.generateToken(
			user.getEmail(),
			user.getRole().name(),
			user.getDisplayName(),
			user.getId().toString()
		);
		String refreshToken = createRefreshToken(user.getId());

		return new TokenPair(accessToken, refreshToken, user);
	}

	@Transactional
	public TokenPair refresh(String refreshTokenValue) {
		String redisKey = redisKeyPrefix + refreshTokenValue;
		String userIdStr = redisTemplate.opsForValue().get(redisKey);

		if (userIdStr == null) {
			throw new IdentityException(IdentityException.ErrorCode.INVALID_CREDENTIALS, "Invalid or expired refresh token");
		}

		UUID userId = UUID.fromString(userIdStr);
		UserEntity userEntity = userRepository.findById(userId)
			.orElseThrow(() -> new IdentityException(IdentityException.ErrorCode.USER_NOT_FOUND, "User not found"));

		// Rotate token: delete old one and create new one
		redisTemplate.delete(redisKey);
		String newRefreshToken = createRefreshToken(userId);

		User user = userEntity.toModel();
		String newAccessToken = jwtTokenProvider.generateToken(
			user.getEmail(),
			user.getRole().name(),
			user.getDisplayName(),
			user.getId().toString()
		);

		return new TokenPair(newAccessToken, newRefreshToken, user);
	}

	@Transactional
	public void changePassword(UUID id, String oldPassword, String newPassword) {
		UserEntity entity = userRepository.findById(id)
			.orElseThrow(() -> new IdentityException(IdentityException.ErrorCode.USER_NOT_FOUND, "User not found"));

		if (!passwordEncoder.matches(oldPassword, entity.getPasswordHash())) {
			throw new IdentityException(IdentityException.ErrorCode.INVALID_CREDENTIALS, "Incorrect current password");
		}

		User user = entity.toModel();
		user.changePasswordHash(passwordEncoder.encode(newPassword), Instant.now());
		userRepository.save(UserEntity.fromModel(user));
	}

	private String createRefreshToken(UUID userId) {
		String tokenValue = UUID.randomUUID().toString();
		String redisKey = redisKeyPrefix + tokenValue;

		// Save key in Redis with configured expiration time
		redisTemplate.opsForValue().set(redisKey, userId.toString(), refreshTokenTtlDays, TimeUnit.DAYS);
		return tokenValue;
	}
}
