package com.vdt.log_monitoring.api.identity;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vdt.log_monitoring.modules.identity.internal.application.ApplicationEntity;
import com.vdt.log_monitoring.modules.identity.internal.application.ApplicationService;
import com.vdt.log_monitoring.modules.identity.internal.metricsource.MetricSourceEntity;
import com.vdt.log_monitoring.modules.identity.internal.metricsource.MetricSourceService;

import lombok.Builder;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/internal/prometheus")
@RequiredArgsConstructor
public class PrometheusTargetController {

	private final MetricSourceService metricSourceService;
	private final ApplicationService applicationService;

	@Builder
	public record PrometheusTargetDto(
		List<String> targets,
		Map<String, String> labels
	) {}

	@GetMapping("/targets")
	public ResponseEntity<List<PrometheusTargetDto>> getTargets() {
		List<MetricSourceEntity> sources = metricSourceService.findAll().stream()
			.filter(MetricSourceEntity::isEnabled)
			.toList();

		List<PrometheusTargetDto> targets = sources.stream().map(source -> {
			ApplicationEntity app = applicationService.getApplicationById(source.getApplicationId());
			
			return PrometheusTargetDto.builder()
				.targets(List.of(source.getTargetHost() + ":" + source.getTargetPort()))
				.labels(Map.of(
					"__metrics_path__", source.getMetricsPath(),
					"application_id", app.getId().toString(),
					"application_name", app.getName()
				))
				.build();
		}).toList();

		return ResponseEntity.ok(targets);
	}
}
