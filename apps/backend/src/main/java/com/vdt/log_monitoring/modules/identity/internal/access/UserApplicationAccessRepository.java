package com.vdt.log_monitoring.modules.identity.internal.access;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserApplicationAccessRepository
		extends JpaRepository<UserApplicationAccessEntity, UserApplicationAccessId> {

	List<UserApplicationAccessEntity> findByIdUserId(UUID userId);

	List<UserApplicationAccessEntity> findByIdApplicationId(UUID applicationId);

	void deleteByIdUserId(UUID userId);
}
