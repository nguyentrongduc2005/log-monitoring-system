package com.vdt.log_monitoring.modules.retention.internal;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RetentionPolicyRepository extends JpaRepository<RetentionPolicyEntity, UUID> {

	List<RetentionPolicyEntity> findAllByOrderBySortOrderAsc();

	List<RetentionPolicyEntity> findByEnabledTrueOrderBySortOrderAsc();
}
