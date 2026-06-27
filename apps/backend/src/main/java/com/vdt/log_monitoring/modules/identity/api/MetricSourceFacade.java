package com.vdt.log_monitoring.modules.identity.api;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface MetricSourceFacade {

	Optional<MetricSourceDto> findByApplicationId(UUID applicationId);

	MetricSourceDto save(
		UUID applicationId,
		String targetHost,
		Integer targetPort,
		String metricsPath,
		String scrapeInterval,
		boolean enabled
	);

	void deleteByApplicationId(UUID applicationId);

	boolean testConnection(String targetHost, Integer targetPort, String metricsPath);

	record MetricSourceDto(
		UUID id,
		UUID applicationId,
		String targetHost,
		Integer targetPort,
		String metricsPath,
		String scrapeInterval,
		boolean enabled,
		Instant createdAt,
		Instant updatedAt
	) {}
}
