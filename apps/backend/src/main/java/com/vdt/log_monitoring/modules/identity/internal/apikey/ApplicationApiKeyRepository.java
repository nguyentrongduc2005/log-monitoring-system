package com.vdt.log_monitoring.modules.identity.internal.apikey;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationApiKeyRepository extends JpaRepository<ApplicationApiKeyEntity, UUID> {

	Optional<ApplicationApiKeyEntity> findByKeyPrefix(String keyPrefix);

	List<ApplicationApiKeyEntity> findByApplicationId(UUID applicationId);

	List<ApplicationApiKeyEntity> findByApplicationIdAndStatus(UUID applicationId, ApiKeyStatus status);
}
