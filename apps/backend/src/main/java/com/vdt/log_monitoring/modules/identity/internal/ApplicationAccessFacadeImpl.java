package com.vdt.log_monitoring.modules.identity.internal;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.identity.api.ApplicationAccessFacade;
import com.vdt.log_monitoring.modules.identity.api.IdentityException;
import com.vdt.log_monitoring.modules.identity.internal.access.ApplicationAccessService;
import com.vdt.log_monitoring.modules.identity.internal.access.UserApplicationAccessEntity;
import com.vdt.log_monitoring.modules.identity.internal.apikey.ApiKeyService;
import com.vdt.log_monitoring.modules.identity.internal.apikey.ApplicationApiKeyEntity;
import com.vdt.log_monitoring.modules.identity.internal.application.ApplicationEntity;
import com.vdt.log_monitoring.modules.identity.internal.application.ApplicationService;

@Component
@RequiredArgsConstructor
public class ApplicationAccessFacadeImpl implements ApplicationAccessFacade {

	private final ApplicationService applicationService;
	private final ApplicationAccessService accessService;
	private final ApiKeyService apiKeyService;

	@Override
	public ApplicationDto createApplication(CreateApplicationCommand command) {
		return mapApplication(applicationService.createApplication(
			command.name(),
			command.displayName(),
			command.description(),
			command.createdBy()
		));
	}

	@Override
	public ApplicationDto updateApplication(UUID applicationId, UpdateApplicationCommand command) {
		return mapApplication(applicationService.updateApplication(
			applicationId,
			command.name(),
			command.displayName(),
			command.description()
		));
	}

	@Override
	public ApplicationDto changeApplicationStatus(UUID applicationId, String status) {
		return mapApplication(applicationService.changeStatus(applicationId, status));
	}

	@Override
	public ApplicationDto findApplicationById(UUID applicationId) {
		return mapApplication(applicationService.getApplicationById(applicationId));
	}

	@Override
	public List<ApplicationDto> findAllApplications() {
		return applicationService.listApplications().stream()
			.map(this::mapApplication)
			.toList();
	}

	@Override
	public List<ApplicationDto> findVisibleApplications(UUID userId, String role) {
		return switch (normalizeRole(role)) {
			case "ADMIN" -> applicationService.listActiveApplications().stream()
				.map(this::mapApplication)
				.toList();
			case "ENGINEER" -> applicationService.listActiveApplicationsVisibleTo(userId).stream()
				.map(this::mapApplication)
				.toList();
			default -> throw new IdentityException(
				IdentityException.ErrorCode.UNAUTHORIZED,
				"Unsupported role"
			);
		};
	}

	@Override
	public List<ApplicationAccessDto> findUserApplicationAccess(UUID userId) {
		return mapAccesses(accessService.findUserApplicationAccess(userId));
	}

	@Override
	public List<ApplicationAccessDto> replaceUserApplicationAccess(
		UUID userId,
		List<ApplicationAccessGrantCommand> grants,
		UUID grantedBy
	) {
		List<ApplicationAccessService.AccessGrant> accessGrants = grants == null
			? List.of()
			: grants.stream()
				.map(grant -> grant == null
					? null
					: new ApplicationAccessService.AccessGrant(grant.applicationId(), grant.accessLevel()))
				.toList();
		return mapAccesses(accessService.replaceUserApplicationAccess(userId, accessGrants, grantedBy));
	}

	@Override
	public ApplicationAccessDto grantUserApplicationAccess(
		UUID userId,
		UUID applicationId,
		String accessLevel,
		UUID grantedBy
	) {
		UserApplicationAccessEntity access =
			accessService.grantUserApplicationAccess(userId, applicationId, accessLevel, grantedBy);
		return mapAccess(access, applicationService.getApplicationById(applicationId));
	}

	@Override
	public void removeUserApplicationAccess(UUID userId, UUID applicationId) {
		accessService.removeUserApplicationAccess(userId, applicationId);
	}

	@Override
	public boolean canViewApplication(UUID userId, UUID applicationId) {
		return accessService.canViewApplication(userId, applicationId);
	}

	@Override
	public boolean canManageApplication(UUID userId, UUID applicationId) {
		return accessService.canManageApplication(userId, applicationId);
	}

	@Override
	public ApiKeyCreationDto createApiKey(UUID applicationId, String name, java.time.Instant expiresAt, UUID createdBy) {
		return mapApiKeyCreation(apiKeyService.createApiKey(applicationId, name, expiresAt, createdBy));
	}

	@Override
	public List<ApiKeyDto> findApiKeys(UUID applicationId) {
		return apiKeyService.findApiKeys(applicationId).stream()
			.map(this::mapApiKey)
			.toList();
	}

	@Override
	public ApiKeyCreationDto rotateApiKey(UUID applicationId, UUID apiKeyId, UUID rotatedBy) {
		return mapApiKeyCreation(apiKeyService.rotateApiKey(applicationId, apiKeyId, rotatedBy));
	}

	@Override
	public void revokeApiKey(UUID applicationId, UUID apiKeyId, UUID revokedBy) {
		apiKeyService.revokeApiKey(applicationId, apiKeyId, revokedBy);
	}

	@Override
	public ApiKeyVerificationDto verifyApplicationApiKey(String rawApiKey) {
		ApiKeyService.ApiKeyVerification verification = apiKeyService.verifyApplicationApiKey(rawApiKey);
		return new ApiKeyVerificationDto(
			verification.valid(),
			verification.applicationId(),
			verification.applicationName(),
			verification.applicationDisplayName(),
			verification.failureReason()
		);
	}

	private ApplicationDto mapApplication(ApplicationEntity application) {
		return new ApplicationDto(
			application.getId(),
			application.getName(),
			application.getDisplayName(),
			application.getDescription(),
			application.getStatus().name(),
			application.getCreatedAt(),
			application.getUpdatedAt()
		);
	}

	private List<ApplicationAccessDto> mapAccesses(List<UserApplicationAccessEntity> accesses) {
		Map<UUID, ApplicationEntity> applicationsById = applicationService.findApplicationsByIds(
			accesses.stream()
				.map(access -> access.getId().getApplicationId())
				.collect(Collectors.toSet())
		).stream().collect(Collectors.toMap(ApplicationEntity::getId, Function.identity()));

		return accesses.stream()
			.map(access -> mapAccess(
				access,
				requireApplication(applicationsById, access.getId().getApplicationId())
			))
			.toList();
	}

	private ApplicationAccessDto mapAccess(
		UserApplicationAccessEntity access,
		ApplicationEntity application
	) {
		return new ApplicationAccessDto(
			access.getId().getUserId(),
			access.getId().getApplicationId(),
			application.getName(),
			application.getDisplayName(),
			access.getAccessLevel().name(),
			access.getGrantedBy(),
			access.getCreatedAt(),
			access.getUpdatedAt()
		);
	}

	private ApiKeyDto mapApiKey(ApplicationApiKeyEntity apiKey) {
		return new ApiKeyDto(
			apiKey.getId(),
			apiKey.getApplicationId(),
			apiKey.getName(),
			apiKey.getKeyPrefix(),
			apiKey.getStatus().name(),
			apiKey.getExpiresAt(),
			apiKey.getLastUsedAt(),
			apiKey.getCreatedAt(),
			apiKey.getRevokedAt()
		);
	}

	private ApiKeyCreationDto mapApiKeyCreation(ApiKeyService.ApiKeyCreation creation) {
		ApplicationApiKeyEntity apiKey = creation.apiKey();
		return new ApiKeyCreationDto(
			apiKey.getId(),
			apiKey.getApplicationId(),
			apiKey.getName(),
			apiKey.getKeyPrefix(),
			creation.rawApiKey(),
			apiKey.getStatus().name(),
			apiKey.getExpiresAt(),
			apiKey.getCreatedAt()
		);
	}

	private ApplicationEntity requireApplication(
		Map<UUID, ApplicationEntity> applicationsById,
		UUID applicationId
	) {
		ApplicationEntity application = applicationsById.get(applicationId);
		if (application == null) {
			throw new IdentityException(
				IdentityException.ErrorCode.APPLICATION_NOT_FOUND,
				"Application not found"
			);
		}
		return application;
	}

	private String normalizeRole(String role) {
		if (role == null || role.isBlank()) {
			throw new IdentityException(
				IdentityException.ErrorCode.UNAUTHORIZED,
				"Unsupported role"
			);
		}
		return role.trim().toUpperCase(Locale.ROOT);
	}
}
