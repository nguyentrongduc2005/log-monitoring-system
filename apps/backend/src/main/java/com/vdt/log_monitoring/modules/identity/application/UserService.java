package com.vdt.log_monitoring.modules.identity.application;

import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vdt.log_monitoring.modules.identity.infrastructure.persistence.UserEntity;
import com.vdt.log_monitoring.modules.identity.infrastructure.persistence.UserRepository;
import com.vdt.log_monitoring.modules.identity.model.User;
import com.vdt.log_monitoring.modules.identity.model.UserRole;
import com.vdt.log_monitoring.modules.identity.model.UserStatus;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public User createUser(String email, String rawPassword, String displayName, UserRole role) {
		String normalizedEmail = email.trim().toLowerCase();
		if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
			throw new IdentityException(IdentityException.ErrorCode.EMAIL_ALREADY_EXISTS, "Email is already registered");
		}

		String encodedPassword = passwordEncoder.encode(rawPassword);
		User user = User.create(normalizedEmail, encodedPassword, displayName, role, Instant.now());
		UserEntity entity = UserEntity.fromModel(user);
		return userRepository.save(entity).toModel();
	}

	public User getUserById(UUID id) {
		return userRepository.findById(id)
			.map(UserEntity::toModel)
			.orElseThrow(() -> new IdentityException(IdentityException.ErrorCode.USER_NOT_FOUND, "User not found"));
	}

	public User getUserByEmail(String email) {
		return userRepository.findByEmailIgnoreCase(email)
			.map(UserEntity::toModel)
			.orElseThrow(() -> new IdentityException(IdentityException.ErrorCode.USER_NOT_FOUND, "User not found"));
	}

	@Transactional
	public User updateProfile(UUID id, String email, String displayName) {
		UserEntity entity = userRepository.findById(id)
			.orElseThrow(() -> new IdentityException(IdentityException.ErrorCode.USER_NOT_FOUND, "User not found"));

		String normalizedEmail = email.trim().toLowerCase();
		if (!entity.getEmail().equalsIgnoreCase(normalizedEmail) && userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
			throw new IdentityException(IdentityException.ErrorCode.EMAIL_ALREADY_EXISTS, "Email is already registered");
		}

		User user = entity.toModel();
		user.updateProfile(normalizedEmail, displayName, Instant.now());

		UserEntity updatedEntity = UserEntity.fromModel(user);
		return userRepository.save(updatedEntity).toModel();
	}

	@Transactional
	public User changeRole(UUID id, UserRole role) {
		UserEntity entity = userRepository.findById(id)
			.orElseThrow(() -> new IdentityException(IdentityException.ErrorCode.USER_NOT_FOUND, "User not found"));

		User user = entity.toModel();
		user.changeRole(role, Instant.now());
		return userRepository.save(UserEntity.fromModel(user)).toModel();
	}

	@Transactional
	public User changeStatus(UUID id, UserStatus status) {
		UserEntity entity = userRepository.findById(id)
			.orElseThrow(() -> new IdentityException(IdentityException.ErrorCode.USER_NOT_FOUND, "User not found"));

		User user = entity.toModel();
		user.changeStatus(status, Instant.now());
		return userRepository.save(UserEntity.fromModel(user)).toModel();
	}
}
