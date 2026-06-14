package com.vdt.log_monitoring.modules.identity.internal.access;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vdt.log_monitoring.modules.identity.api.IdentityException;
import com.vdt.log_monitoring.modules.identity.internal.application.ApplicationEntity;
import com.vdt.log_monitoring.modules.identity.internal.application.ApplicationRepository;
import com.vdt.log_monitoring.modules.identity.internal.application.ApplicationStatus;
import com.vdt.log_monitoring.modules.identity.internal.user.UserEntity;
import com.vdt.log_monitoring.modules.identity.internal.user.UserRepository;
import com.vdt.log_monitoring.modules.identity.internal.user.UserRole;

@Service
@RequiredArgsConstructor
public class ApplicationAccessService {

	private final UserRepository userRepository;
	private final ApplicationRepository applicationRepository;
	private final UserApplicationAccessRepository accessRepository;

	@Transactional(readOnly = true)
	public List<UserApplicationAccessEntity> findUserApplicationAccess(UUID userId) {
		validateUserExists(userId);
		return accessRepository.findByIdUserId(userId);
	}

	@Transactional
	public List<UserApplicationAccessEntity> replaceUserApplicationAccess(
		UUID userId,
		List<AccessGrant> grants,
		UUID grantedBy
	) {
		List<AccessGrant> safeGrants = grants == null ? List.of() : grants;
		validateNoDuplicateApplicationIds(safeGrants);
		validateUserExists(userId);
		validateUserExists(grantedBy);

		accessRepository.deleteByIdUserId(userId);
		return safeGrants.stream()
			.map(grant -> createAccess(userId, grant.applicationId(), parseAccessLevel(grant.accessLevel()), grantedBy))
			.map(accessRepository::save)
			.toList();
	}

	@Transactional
	public UserApplicationAccessEntity grantUserApplicationAccess(
		UUID userId,
		UUID applicationId,
		String accessLevel,
		UUID grantedBy
	) {
		validateUserExists(userId);
		validateUserExists(grantedBy);
		ApplicationAccessLevel level = parseAccessLevel(accessLevel);
		validateActiveApplication(applicationId);

		UserApplicationAccessId id = new UserApplicationAccessId(userId, applicationId);
		UserApplicationAccessEntity access = accessRepository.findById(id)
			.orElseGet(() -> UserApplicationAccessEntity.create(userId, applicationId, level, grantedBy));
		access.changeAccessLevel(level, grantedBy);
		return accessRepository.save(access);
	}

	@Transactional
	public void removeUserApplicationAccess(UUID userId, UUID applicationId) {
		accessRepository.deleteById(new UserApplicationAccessId(userId, applicationId));
	}

	@Transactional(readOnly = true)
	public boolean canViewApplication(UUID userId, UUID applicationId) {
		UserEntity user = getUser(userId);
		validateApplicationExists(applicationId);
		if (user.getRole() == UserRole.ADMIN) {
			return true;
		}

		return accessRepository.findById(new UserApplicationAccessId(userId, applicationId))
			.map(UserApplicationAccessEntity::getAccessLevel)
			.map(level -> level == ApplicationAccessLevel.VIEW || level == ApplicationAccessLevel.MANAGE)
			.orElse(false);
	}

	@Transactional(readOnly = true)
	public boolean canManageApplication(UUID userId, UUID applicationId) {
		UserEntity user = getUser(userId);
		validateApplicationExists(applicationId);
		if (user.getRole() == UserRole.ADMIN) {
			return true;
		}

		return accessRepository.findById(new UserApplicationAccessId(userId, applicationId))
			.map(UserApplicationAccessEntity::getAccessLevel)
			.map(level -> level == ApplicationAccessLevel.MANAGE)
			.orElse(false);
	}

	private UserApplicationAccessEntity createAccess(
		UUID userId,
		UUID applicationId,
		ApplicationAccessLevel accessLevel,
		UUID grantedBy
	) {
		validateActiveApplication(applicationId);
		return UserApplicationAccessEntity.create(userId, applicationId, accessLevel, grantedBy);
	}

	private void validateNoDuplicateApplicationIds(
		List<AccessGrant> grants
	) {
		Set<UUID> applicationIds = new HashSet<>();
		for (AccessGrant grant : grants) {
			if (grant == null || grant.applicationId() == null || !applicationIds.add(grant.applicationId())) {
				throw new IdentityException(
					IdentityException.ErrorCode.INVALID_APPLICATION_ACCESS_GRANT,
					"Application access grants must contain unique application ids"
				);
			}
		}
	}

	private ApplicationAccessLevel parseAccessLevel(String accessLevel) {
		try {
			return ApplicationAccessLevel.valueOf(requireText(accessLevel, "accessLevel").toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ex) {
			throw new IdentityException(
				IdentityException.ErrorCode.INVALID_APPLICATION_ACCESS_LEVEL,
				"Invalid application access level"
			);
		}
	}

	private UserEntity getUser(UUID userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new IdentityException(
				IdentityException.ErrorCode.USER_NOT_FOUND,
				"User not found"
			));
	}

	private void validateUserExists(UUID userId) {
		getUser(userId);
	}

	private ApplicationEntity validateApplicationExists(UUID applicationId) {
		return applicationRepository.findById(applicationId)
			.orElseThrow(() -> new IdentityException(
				IdentityException.ErrorCode.APPLICATION_NOT_FOUND,
				"Application not found"
			));
	}

	private ApplicationEntity validateActiveApplication(UUID applicationId) {
		ApplicationEntity application = validateApplicationExists(applicationId);
		if (application.getStatus() != ApplicationStatus.ACTIVE) {
			throw new IdentityException(
				IdentityException.ErrorCode.APPLICATION_INACTIVE,
				"Application is inactive"
			);
		}
		return application;
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return value.trim();
	}

	public record AccessGrant(UUID applicationId, String accessLevel) {}
}
