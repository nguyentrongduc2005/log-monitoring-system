package com.vdt.log_monitoring.modules.retention.internal;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RetentionRunRepository extends JpaRepository<RetentionRunEntity, UUID> {

	Optional<RetentionRunEntity> findFirstByPolicyIdOrderByStartedAtDesc(UUID policyId);
}
