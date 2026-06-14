package com.vdt.log_monitoring.modules.identity.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ApplicationAccessFacade {

	ApplicationDto createApplication(CreateApplicationCommand command);

	ApplicationDto updateApplication(UUID applicationId, UpdateApplicationCommand command);

	ApplicationDto changeApplicationStatus(UUID applicationId, String status);

	ApplicationDto findApplicationById(UUID applicationId);

	List<ApplicationDto> findAllApplications();

	List<ApplicationDto> findVisibleApplications(UUID userId, String role);

	List<ApplicationAccessDto> findUserApplicationAccess(UUID userId);

	List<ApplicationAccessDto> replaceUserApplicationAccess(
			UUID userId,
			List<ApplicationAccessGrantCommand> grants,
			UUID grantedBy);

	ApplicationAccessDto grantUserApplicationAccess(
			UUID userId,
			UUID applicationId,
			String accessLevel,
			UUID grantedBy);

	void removeUserApplicationAccess(UUID userId, UUID applicationId);

	boolean canViewApplication(UUID userId, UUID applicationId);

	boolean canManageApplication(UUID userId, UUID applicationId);

	ApiKeyCreationDto createApiKey(UUID applicationId, String name, Instant expiresAt, UUID createdBy);

	List<ApiKeyDto> findApiKeys(UUID applicationId);

	ApiKeyCreationDto rotateApiKey(UUID applicationId, UUID apiKeyId, UUID rotatedBy);

	void revokeApiKey(UUID applicationId, UUID apiKeyId, UUID revokedBy);

	ApiKeyVerificationDto verifyApplicationApiKey(String rawApiKey);

	record CreateApplicationCommand(
			String name,
			String displayName,
			String description,
			UUID createdBy) {
	}

	record UpdateApplicationCommand(
			String name,
			String displayName,
			String description) {
	}

	record ApplicationAccessGrantCommand(
			UUID applicationId,
			String accessLevel) {
	}

	record ApplicationDto(
			UUID id,
			String name,
			String displayName,
			String description,
			String status,
			Instant createdAt,
			Instant updatedAt) {
	}

	record ApplicationAccessDto(
			UUID userId,
			UUID applicationId,
			String applicationName,
			String applicationDisplayName,
			String accessLevel,
			UUID grantedBy,
			Instant createdAt,
			Instant updatedAt) {
	}

	record ApiKeyDto(
			UUID id,
			UUID applicationId,
			String name,
			String keyPrefix,
			String status,
			Instant expiresAt,
			Instant lastUsedAt,
			Instant createdAt,
			Instant revokedAt) {
	}

	record ApiKeyCreationDto(
			UUID id,
			UUID applicationId,
			String name,
			String keyPrefix,
			String rawApiKey,
			String status,
			Instant expiresAt,
			Instant createdAt) {
	}

	record ApiKeyVerificationDto(
			boolean valid,
			UUID applicationId,
			String applicationName,
			String applicationDisplayName,
			String failureReason) {
	}

}
