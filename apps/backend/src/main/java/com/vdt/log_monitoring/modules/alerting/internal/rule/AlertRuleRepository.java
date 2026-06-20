package com.vdt.log_monitoring.modules.alerting.internal.rule;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRuleEntity, UUID> {

	boolean existsByApplicationIdAndNameIgnoreCase(UUID applicationId, String name);

	List<AlertRuleEntity> findByApplicationId(UUID applicationId);

	List<AlertRuleEntity> findByApplicationIdAndStatus(
			UUID applicationId,
			AlertRuleStatus status);
}
