package com.vdt.log_monitoring.modules.alerting.internal.alert;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AlertRepository extends JpaRepository<AlertEntity, UUID> {

	List<AlertEntity> findByApplicationIdAndStatusOrderByTriggeredAtDesc(
			UUID applicationId,
			AlertStatus status);

	List<AlertEntity> findByRuleIdOrderByTriggeredAtDesc(UUID ruleId);

	List<AlertEntity> findByApplicationIdInOrderByTriggeredAtDesc(List<UUID> applicationIds);

	List<AlertEntity> findByApplicationIdAndFirstSeenAtBetweenOrderByTriggeredAtDesc(
			UUID applicationId,
			Instant windowStart,
			Instant windowEnd);

	Optional<AlertEntity> findFirstByRuleIdAndApplicationIdAndStatusNotOrderByTriggeredAtDesc(
			UUID ruleId,
			UUID applicationId,
			AlertStatus excludedStatus);

	@Modifying
	@Query("UPDATE AlertEntity a SET a.occurrenceCount = :occurrenceCount, a.lastSeenAt = :lastSeenAt, a.updatedAt = :updatedAt " +
			"WHERE a.ruleId = :ruleId AND a.applicationId = :applicationId AND a.status <> com.vdt.log_monitoring.modules.alerting.internal.alert.AlertStatus.RESOLVED")
	int updateOccurrenceCountAndLastSeenAt(
			@Param("ruleId") UUID ruleId,
			@Param("applicationId") UUID applicationId,
			@Param("occurrenceCount") long occurrenceCount,
			@Param("lastSeenAt") Instant lastSeenAt,
			@Param("updatedAt") Instant updatedAt);
}
