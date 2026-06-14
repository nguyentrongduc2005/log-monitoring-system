package com.vdt.log_monitoring.modules.identity.internal;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.identity.api.IdentityFacade;
import com.vdt.log_monitoring.modules.identity.internal.auth.AuthService;
import com.vdt.log_monitoring.modules.identity.internal.auth.TokenPair;
import com.vdt.log_monitoring.modules.identity.internal.user.UserEntity;
import com.vdt.log_monitoring.modules.identity.internal.user.UserRole;
import com.vdt.log_monitoring.modules.identity.internal.user.UserService;
import com.vdt.log_monitoring.modules.identity.internal.user.UserStatus;

@Component
@RequiredArgsConstructor
public class IdentityFacadeImpl implements IdentityFacade {

	private final UserService userService;
	private final AuthService authService;

	@Override
	public UserDto createUser(String email, String rawPassword, String displayName, String role) {
		UserRole userRole = UserRole.valueOf(role.toUpperCase());
		UserEntity user = userService.createUser(email, rawPassword, displayName, userRole);
		return mapToDto(user);
	}

	@Override
	public List<UserDto> findUsers() {
		return findUsers(new UserSearchQuery(0, 100, null, null, null, false)).users();
	}

	@Override
	public UserPageDto findUsers(UserSearchQuery query) {
		UserRole role = query.role() == null || query.role().isBlank()
			? null
			: UserRole.valueOf(query.role().toUpperCase());
		UserStatus status = query.status() == null || query.status().isBlank()
			? null
			: UserStatus.valueOf(query.status().toUpperCase());
		int page = Math.max(query.page(), 0);
		int size = Math.max(1, Math.min(query.size(), 100));

		Page<UserEntity> result = userService.listUsers(
			query.search(),
			role,
			status,
			query.includeDeleted(),
			PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
		);

		return new UserPageDto(
			result.getContent().stream().map(this::mapToDto).toList(),
			result.getNumber(),
			result.getSize(),
			result.getTotalElements(),
			result.getTotalPages()
		);
	}

	@Override
	public UserDto findUserById(UUID id) {
		UserEntity user = userService.getUserById(id);
		return mapToDto(user);
	}

	@Override
	public UserDto findUserByEmail(String email) {
		UserEntity user = userService.getUserByEmail(email);
		return mapToDto(user);
	}

	@Override
	public UserDto updateProfile(UUID id, String email, String displayName) {
		UserEntity user = userService.updateProfile(id, email, displayName);
		return mapToDto(user);
	}

	@Override
	public void changePassword(UUID id, String oldPassword, String newPassword) {
		authService.changePassword(id, oldPassword, newPassword);
	}

	@Override
	public UserDto changeRole(UUID id, String role) {
		UserRole userRole = UserRole.valueOf(role.toUpperCase());
		UserEntity user = userService.changeRole(id, userRole);
		return mapToDto(user);
	}

	@Override
	public UserDto changeStatus(UUID id, String status) {
		UserStatus userStatus = UserStatus.valueOf(status.toUpperCase());
		UserEntity user = userService.changeStatus(id, userStatus);
		return mapToDto(user);
	}

	@Override
	public UserDto softDeleteUser(UUID id) {
		UserEntity user = userService.softDelete(id);
		return mapToDto(user);
	}

	@Override
	public TokenPairDto authenticate(String email, String rawPassword) {
		TokenPair tokenPair = authService.authenticate(email, rawPassword);
		return mapToTokenPairDto(tokenPair);
	}

	@Override
	public TokenPairDto refresh(String refreshToken) {
		TokenPair tokenPair = authService.refresh(refreshToken);
		return mapToTokenPairDto(tokenPair);
	}

	@Override
	public void logout(String accessToken, String refreshToken) {
		authService.logout(accessToken, refreshToken);
	}

	private UserDto mapToDto(UserEntity user) {
		return new UserDto(
			user.getId(),
			user.getEmail(),
			user.getDisplayName(),
			user.getRole().name(),
			user.getStatus().name(),
			user.getLastLoginAt(),
			user.getCreatedAt(),
			user.getUpdatedAt()
		);
	}

	private TokenPairDto mapToTokenPairDto(TokenPair tokenPair) {
		return new TokenPairDto(
			tokenPair.accessToken(),
			tokenPair.refreshToken(),
			mapToDto(tokenPair.user())
		);
	}
}
