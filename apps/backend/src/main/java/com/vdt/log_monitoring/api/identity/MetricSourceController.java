package com.vdt.log_monitoring.api.identity;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vdt.log_monitoring.api.identity.dto.MetricSourceDto.MetricSourceRequest;
import com.vdt.log_monitoring.api.identity.dto.MetricSourceDto.MetricSourceResponse;
import com.vdt.log_monitoring.modules.identity.internal.metricsource.MetricSourceEntity;
import com.vdt.log_monitoring.modules.identity.internal.metricsource.MetricSourceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MetricSourceController {

	private final MetricSourceService service;

	@GetMapping("/applications/{applicationId}/metric-sources")
	public ResponseEntity<MetricSourceResponse> getMetricSource(@PathVariable UUID applicationId) {
		return service.findByApplicationId(applicationId)
			.map(this::mapToResponse)
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.noContent().build());
	}

	@PostMapping("/applications/{applicationId}/metric-sources")
	public ResponseEntity<MetricSourceResponse> saveMetricSource(
		@PathVariable UUID applicationId,
		@RequestBody @Valid MetricSourceRequest request
	) {
		MetricSourceEntity saved = service.save(
			applicationId,
			request.targetHost(),
			request.targetPort(),
			request.metricsPath(),
			request.scrapeInterval(),
			request.enabled()
		);
		return ResponseEntity.ok(mapToResponse(saved));
	}

	@PutMapping("/metric-sources/{applicationId}")
	public ResponseEntity<MetricSourceResponse> updateMetricSource(
		@PathVariable UUID applicationId,
		@RequestBody @Valid MetricSourceRequest request
	) {
		MetricSourceEntity saved = service.save(
			applicationId,
			request.targetHost(),
			request.targetPort(),
			request.metricsPath(),
			request.scrapeInterval(),
			request.enabled()
		);
		return ResponseEntity.ok(mapToResponse(saved));
	}

	@DeleteMapping("/metric-sources/{applicationId}")
	public ResponseEntity<Void> deleteMetricSource(@PathVariable UUID applicationId) {
		service.deleteByApplicationId(applicationId);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/metric-sources/test-connection")
	public ResponseEntity<Boolean> testConnection(@RequestBody @Valid MetricSourceRequest request) {
		boolean isUp = service.testConnection(
			request.targetHost(),
			request.targetPort(),
			request.metricsPath()
		);
		return ResponseEntity.ok(isUp);
	}

	private MetricSourceResponse mapToResponse(MetricSourceEntity entity) {
		return MetricSourceResponse.builder()
			.id(entity.getId())
			.applicationId(entity.getApplicationId())
			.targetHost(entity.getTargetHost())
			.targetPort(entity.getTargetPort())
			.metricsPath(entity.getMetricsPath())
			.scrapeInterval(entity.getScrapeInterval())
			.enabled(entity.isEnabled())
			.createdAt(entity.getCreatedAt())
			.updatedAt(entity.getUpdatedAt())
			.build();
	}
}
