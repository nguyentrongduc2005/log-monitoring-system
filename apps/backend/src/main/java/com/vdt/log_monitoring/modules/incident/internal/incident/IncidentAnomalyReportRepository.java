package com.vdt.log_monitoring.modules.incident.internal.incident;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IncidentAnomalyReportRepository extends JpaRepository<IncidentAnomalyReportEntity, UUID> {
    Optional<IncidentAnomalyReportEntity> findByAlertId(UUID alertId);
}
