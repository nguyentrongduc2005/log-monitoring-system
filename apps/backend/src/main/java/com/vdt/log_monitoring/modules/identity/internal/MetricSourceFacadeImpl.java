package com.vdt.log_monitoring.modules.identity.internal;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import com.vdt.log_monitoring.modules.identity.api.MetricSourceFacade;
import com.vdt.log_monitoring.modules.identity.internal.metricsource.MetricSourceEntity;
import com.vdt.log_monitoring.modules.identity.internal.metricsource.MetricSourceService;

@Component
@RequiredArgsConstructor
public class MetricSourceFacadeImpl implements MetricSourceFacade {

	private final MetricSourceService service;

	@Override
	public Optional<MetricSourceDto> findByApplicationId(UUID applicationId) {
		return service.findByApplicationId(applicationId).map(this::mapToDto);
	}

	@Override
	public MetricSourceDto save(
		UUID applicationId,
		String targetHost,
		Integer targetPort,
		String metricsPath,
		String scrapeInterval,
		boolean enabled
	) {
		MetricSourceEntity saved = service.save(applicationId, targetHost, targetPort, metricsPath, scrapeInterval, enabled);
		return mapToDto(saved);
	}

	@Override
	public void deleteByApplicationId(UUID applicationId) {
		service.deleteByApplicationId(applicationId);
	}

	@Override
	public boolean testConnection(String targetHost, Integer targetPort, String metricsPath) {
		return service.testConnection(targetHost, targetPort, metricsPath);
	}

	private MetricSourceDto mapToDto(MetricSourceEntity entity) {
		return new MetricSourceDto(
			entity.getId(),
			entity.getApplicationId(),
			entity.getTargetHost(),
			entity.getTargetPort(),
			entity.getMetricsPath(),
			entity.getScrapeInterval(),
			entity.isEnabled(),
			entity.getCreatedAt(),
			entity.getUpdatedAt()
		);
	}
}
