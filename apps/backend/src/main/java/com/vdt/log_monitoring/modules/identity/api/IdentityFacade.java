package com.vdt.log_monitoring.modules.identity.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface IdentityFacade {

	UserDto createUser(String email, String rawPassword, String displayName, String role);

	List<UserDto> findUsers();

	default UserPageDto findUsers(UserSearchQuery query) {
		List<UserDto> users = findUsers();
		int size = users.size();
		return new UserPageDto(users, 0, size, size, size == 0 ? 0 : 1);
	}

	UserDto findUserById(UUID id);

	UserDto findUserByEmail(String email);

	UserDto updateProfile(UUID id, String email, String displayName);

	default UserDto updateUser(UUID id, String email, String displayName) {
		return updateProfile(id, email, displayName);
	}

	void changePassword(UUID id, String oldPassword, String newPassword);

	UserDto changeRole(UUID id, String role);

	UserDto changeStatus(UUID id, String status);

	default UserDto softDeleteUser(UUID id) {
		return changeStatus(id, "DELETED");
	}

	TokenPairDto authenticate(String email, String rawPassword);

	TokenPairDto refresh(String refreshToken);

	void logout(String accessToken, String refreshToken);

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

	record UserSearchQuery(
		int page,
		int size,
		String search,
		String role,
		String status,
		boolean includeDeleted
	) {}

	record UserPageDto(
		List<UserDto> users,
		int page,
		int size,
		long totalElements,
		int totalPages
	) {}

	record TokenPairDto(
		String accessToken,
		String refreshToken,
		UserDto user
	) {}
}
