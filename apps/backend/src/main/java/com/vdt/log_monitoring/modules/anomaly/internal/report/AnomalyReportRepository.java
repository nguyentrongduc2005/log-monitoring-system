package com.vdt.log_monitoring.modules.anomaly.internal.report;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnomalyReportRepository extends JpaRepository<AnomalyReportEntity, UUID> {

	List<AnomalyReportEntity> findByApplicationIdInOrderByCreatedAtDesc(List<UUID> applicationIds);
}
