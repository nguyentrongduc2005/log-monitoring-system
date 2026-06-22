package com.vdt.log_monitoring.modules.alerting.internal.alert;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlertRepository extends JpaRepository<AlertEntity, UUID> {

	List<AlertEntity> findByApplicationIdAndStatusOrderByTriggeredAtDesc(
			UUID applicationId,
			AlertStatus status);

	List<AlertEntity> findByRuleIdOrderByTriggeredAtDesc(UUID ruleId);

	List<AlertEntity> findByApplicationIdInOrderByTriggeredAtDesc(List<UUID> applicationIds);

	Optional<AlertEntity> findFirstByRuleIdAndFingerprintAndStatusNotOrderByTriggeredAtDesc(
			UUID ruleId,
			String fingerprint,
			AlertStatus excludedStatus);
}
