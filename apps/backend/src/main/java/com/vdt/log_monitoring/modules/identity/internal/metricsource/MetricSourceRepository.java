package com.vdt.log_monitoring.modules.identity.internal.metricsource;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MetricSourceRepository extends JpaRepository<MetricSourceEntity, UUID> {
	Optional<MetricSourceEntity> findByApplicationId(UUID applicationId);
}
