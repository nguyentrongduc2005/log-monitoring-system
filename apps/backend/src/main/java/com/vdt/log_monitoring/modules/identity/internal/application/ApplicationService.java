package com.vdt.log_monitoring.modules.identity.internal.application;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vdt.log_monitoring.modules.identity.api.IdentityException;
import com.vdt.log_monitoring.modules.identity.internal.access.UserApplicationAccessRepository;

@Service
@RequiredArgsConstructor
public class ApplicationService {

	private final ApplicationRepository applicationRepository;
	private final UserApplicationAccessRepository userApplicationAccessRepository;

	@Transactional
	public ApplicationEntity createApplication(
			String name,
			String displayName,
			String description,
			UUID createdBy) {
		String normalizedName = requireText(name, "name");
		if (applicationRepository.existsByNameIgnoreCase(normalizedName)) {
			throw new IdentityException(
					IdentityException.ErrorCode.APPLICATION_NAME_ALREADY_EXISTS,
					"Application name is already registered");
		}

		return applicationRepository.save(
				ApplicationEntity.create(normalizedName, displayName, description, createdBy));
	}

	@Transactional
	public ApplicationEntity updateApplication(
			UUID applicationId,
			String name,
			String displayName,
			String description) {
		ApplicationEntity application = getApplicationById(applicationId);
		String normalizedName = requireText(name, "name");
		if (!application.getName().equalsIgnoreCase(normalizedName)
				&& applicationRepository.existsByNameIgnoreCase(normalizedName)) {
			throw new IdentityException(
					IdentityException.ErrorCode.APPLICATION_NAME_ALREADY_EXISTS,
					"Application name is already registered");
		}

		application.update(normalizedName, displayName, description);
		return application;
	}

	@Transactional
	public ApplicationEntity changeStatus(UUID applicationId, String status) {
		ApplicationEntity application = getApplicationById(applicationId);
		application.changeStatus(parseStatus(status));
		return application;
	}

	@Transactional(readOnly = true)
	public ApplicationEntity getApplicationById(UUID applicationId) {
		return applicationRepository.findById(applicationId)
				.orElseThrow(() -> new IdentityException(
						IdentityException.ErrorCode.APPLICATION_NOT_FOUND,
						"Application not found"));
	}

	@Transactional(readOnly = true)
	public List<ApplicationEntity> listApplications() {
		return applicationRepository.findAll();
	}

	@Transactional(readOnly = true)
	public List<ApplicationEntity> listActiveApplications() {
		return applicationRepository.findByStatus(ApplicationStatus.ACTIVE);
	}

	@Transactional(readOnly = true)
	public List<ApplicationEntity> listActiveApplicationsVisibleTo(UUID userId) {
		return applicationRepository.findVisibleByUserIdAndStatus(userId, ApplicationStatus.ACTIVE);
	}

	@Transactional(readOnly = true)
	public List<ApplicationEntity> findApplicationsByIds(Collection<UUID> applicationIds) {
		if (applicationIds == null || applicationIds.isEmpty()) {
			return List.of();
		}
		return applicationRepository.findAllById(applicationIds);
	}

	private ApplicationStatus parseStatus(String status) {
		try {
			return ApplicationStatus.valueOf(requireText(status, "status").toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ex) {
			throw new IdentityException(
					IdentityException.ErrorCode.INVALID_APPLICATION_STATUS,
					"Invalid application status");
		}
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IdentityException(
					IdentityException.ErrorCode.INVALID_APPLICATION_ACCESS_GRANT,
					fieldName + " must not be blank");
		}
		return value.trim();
	}
}
