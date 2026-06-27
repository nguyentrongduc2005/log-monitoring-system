package com.vdt.log_monitoring.modules.identity.internal.metricsource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MetricSourceService {

	private final MetricSourceRepository repository;
	private final RestClient restClient = RestClient.create();

	@Transactional(readOnly = true)
	public List<MetricSourceEntity> findAll() {
		return repository.findAll();
	}

	@Transactional(readOnly = true)
	public Optional<MetricSourceEntity> findByApplicationId(UUID applicationId) {
		return repository.findByApplicationId(applicationId);
	}

	@Transactional
	public MetricSourceEntity save(
		UUID applicationId,
		String targetHost,
		Integer targetPort,
		String metricsPath,
		String scrapeInterval,
		boolean enabled
	) {
		Optional<MetricSourceEntity> existing = repository.findByApplicationId(applicationId);
		
		if (existing.isPresent()) {
			MetricSourceEntity entity = existing.get();
			entity.update(targetHost, targetPort, metricsPath, scrapeInterval, enabled);
			return repository.save(entity);
		}

		MetricSourceEntity newEntity = MetricSourceEntity.create(
			applicationId,
			targetHost,
			targetPort,
			metricsPath,
			scrapeInterval,
			enabled
		);
		return repository.save(newEntity);
	}

	@Transactional
	public void deleteByApplicationId(UUID applicationId) {
		repository.findByApplicationId(applicationId).ifPresent(repository::delete);
	}

	public boolean testConnection(String targetHost, Integer targetPort, String metricsPath) {
		try {
			String url = String.format("http://%s:%d%s", targetHost, targetPort, metricsPath);
			restClient.get()
				.uri(url)
				.retrieve()
				.toBodilessEntity();
			return true;
		} catch (RestClientException | IllegalArgumentException e) {
			return false;
		}
	}
}
