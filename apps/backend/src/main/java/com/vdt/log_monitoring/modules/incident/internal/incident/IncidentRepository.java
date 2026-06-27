package com.vdt.log_monitoring.modules.incident.internal.incident;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IncidentRepository extends JpaRepository<IncidentEntity, UUID> {

	@Query("""
		select distinct incident
		from IncidentEntity incident
		join incident.applications application
		where application.id.applicationId in :applicationIds
		and (:status is null or incident.status = :status)
		and (:severity is null or incident.severity = :severity)
		order by incident.startedAt desc
		""")
	List<IncidentEntity> findVisible(
		@Param("applicationIds") List<UUID> applicationIds,
		@Param("status") IncidentStatus status,
		@Param("severity") IncidentSeverity severity);

	@Query("""
		select incident
		from IncidentEntity incident
		join incident.alerts alert
		where alert.id.alertId = :alertId
		and incident.status <> com.vdt.log_monitoring.modules.incident.internal.incident.IncidentStatus.RESOLVED
		order by incident.startedAt desc
		""")
	List<IncidentEntity> findOpenByAlertId(@Param("alertId") UUID alertId);

	@Query("""
		select distinct incident
		from IncidentEntity incident
		join incident.applications application
		join incident.evidence evidence
		where application.id.applicationId = :applicationId
		and evidence.fingerprint = :fingerprint
		and incident.status <> com.vdt.log_monitoring.modules.incident.internal.incident.IncidentStatus.RESOLVED
		order by incident.startedAt desc
		""")
	List<IncidentEntity> findOpenByApplicationIdAndFingerprint(
		@Param("applicationId") UUID applicationId,
		@Param("fingerprint") String fingerprint);
}
