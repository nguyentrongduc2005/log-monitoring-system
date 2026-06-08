package com.vdt.log_monitoring.modules.identity.application;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.identity.api.IdentityFacade;
import com.vdt.log_monitoring.modules.identity.model.User;
import com.vdt.log_monitoring.modules.identity.model.UserRole;
import com.vdt.log_monitoring.modules.identity.model.UserStatus;

@Component
@RequiredArgsConstructor
public class IdentityFacadeImpl implements IdentityFacade {

	private final UserService userService;
	private final AuthService authService;

	@Override
	public UserDto createUser(String email, String rawPassword, String displayName, String role) {
		UserRole userRole = UserRole.valueOf(role.toUpperCase());
		User user = userService.createUser(email, rawPassword, displayName, userRole);
		return mapToDto(user);
	}

	@Override
	public UserDto findUserById(UUID id) {
		User user = userService.getUserById(id);
		return mapToDto(user);
	}

	@Override
	public UserDto findUserByEmail(String email) {
		User user = userService.getUserByEmail(email);
		return mapToDto(user);
	}

	@Override
	public UserDto updateProfile(UUID id, String email, String displayName) {
		User user = userService.updateProfile(id, email, displayName);
		return mapToDto(user);
	}

	@Override
	public void changePassword(UUID id, String oldPassword, String newPassword) {
		authService.changePassword(id, oldPassword, newPassword);
	}

	@Override
	public UserDto changeRole(UUID id, String role) {
		UserRole userRole = UserRole.valueOf(role.toUpperCase());
		User user = userService.changeRole(id, userRole);
		return mapToDto(user);
	}

	@Override
	public UserDto changeStatus(UUID id, String status) {
		UserStatus userStatus = UserStatus.valueOf(status.toUpperCase());
		User user = userService.changeStatus(id, userStatus);
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

	private UserDto mapToDto(User user) {
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
