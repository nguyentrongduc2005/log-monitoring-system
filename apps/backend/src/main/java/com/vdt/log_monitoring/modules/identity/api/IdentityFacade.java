package com.vdt.log_monitoring.modules.identity.api;

import java.time.Instant;
import java.util.UUID;

public interface IdentityFacade {

	UserDto createUser(String email, String rawPassword, String displayName, String role);

	UserDto findUserById(UUID id);

	UserDto findUserByEmail(String email);

	UserDto updateProfile(UUID id, String email, String displayName);

	void changePassword(UUID id, String oldPassword, String newPassword);

	UserDto changeRole(UUID id, String role);

	UserDto changeStatus(UUID id, String status);

	TokenPairDto authenticate(String email, String rawPassword);

	TokenPairDto refresh(String refreshToken);

	record UserDto(
		UUID id,
		String email,
		String displayName,
		String role,
		String status,
		Instant lastLoginAt,
		Instant createdAt,
		Instant updatedAt
	) {}

	record TokenPairDto(
		String accessToken,
		String refreshToken,
		UserDto user
	) {}
}
