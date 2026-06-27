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
import com.vdt.log_monitoring.modules.identity.api.MetricSourceFacade;
import com.vdt.log_monitoring.modules.identity.api.MetricSourceFacade.MetricSourceDto;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import com.vdt.log_monitoring.shared.dto.ApiResponse;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MetricSourceController {

	private final MetricSourceFacade facade;

	@GetMapping("/applications/{applicationId}/metric-sources")
	public ResponseEntity<ApiResponse<MetricSourceResponse>> getMetricSource(@PathVariable UUID applicationId) {
		return facade.findByApplicationId(applicationId)
			.map(this::mapToResponse)
			.map(ApiResponse::success)
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.noContent().build());
	}

	@PostMapping("/applications/{applicationId}/metric-sources")
	public ResponseEntity<ApiResponse<MetricSourceResponse>> saveMetricSource(
		@PathVariable UUID applicationId,
		@RequestBody @Valid MetricSourceRequest request
	) {
		MetricSourceDto saved = facade.save(
			applicationId,
			request.targetHost(),
			request.targetPort(),
			request.metricsPath(),
			request.scrapeInterval(),
			request.enabled()
		);
		return ResponseEntity.ok(ApiResponse.success(mapToResponse(saved)));
	}

	@PutMapping("/metric-sources/{applicationId}")
	public ResponseEntity<ApiResponse<MetricSourceResponse>> updateMetricSource(
		@PathVariable UUID applicationId,
		@RequestBody @Valid MetricSourceRequest request
	) {
		MetricSourceDto saved = facade.save(
			applicationId,
			request.targetHost(),
			request.targetPort(),
			request.metricsPath(),
			request.scrapeInterval(),
			request.enabled()
		);
		return ResponseEntity.ok(ApiResponse.success(mapToResponse(saved)));
	}

	@DeleteMapping("/metric-sources/{applicationId}")
	public ResponseEntity<Void> deleteMetricSource(@PathVariable UUID applicationId) {
		facade.deleteByApplicationId(applicationId);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/metric-sources/test-connection")
	public ResponseEntity<ApiResponse<Boolean>> testConnection(@RequestBody @Valid MetricSourceRequest request) {
		boolean isUp = facade.testConnection(
			request.targetHost(),
			request.targetPort(),
			request.metricsPath()
		);
		return ResponseEntity.ok(ApiResponse.success(isUp));
	}

	private MetricSourceResponse mapToResponse(MetricSourceDto dto) {
		return MetricSourceResponse.builder()
			.id(dto.id())
			.applicationId(dto.applicationId())
			.targetHost(dto.targetHost())
			.targetPort(dto.targetPort())
			.metricsPath(dto.metricsPath())
			.scrapeInterval(dto.scrapeInterval())
			.enabled(dto.enabled())
			.createdAt(dto.createdAt())
			.updatedAt(dto.updatedAt())
			.build();
	}
}
