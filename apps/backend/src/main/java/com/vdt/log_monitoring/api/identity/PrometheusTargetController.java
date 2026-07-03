package com.vdt.log_monitoring.api.identity;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vdt.log_monitoring.modules.identity.api.ApplicationAccessFacade;
import com.vdt.log_monitoring.modules.identity.api.MetricSourceFacade;

import lombok.Builder;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/internal/prometheus")
@RequiredArgsConstructor
public class PrometheusTargetController {

	private final MetricSourceFacade metricSourceFacade;
	private final ApplicationAccessFacade applicationAccessFacade;

	@Builder
	public record PrometheusTargetDto(
		List<String> targets,
		Map<String, String> labels
	) {}

	@GetMapping("/targets")
	public ResponseEntity<List<PrometheusTargetDto>> getTargets() {
		List<MetricSourceFacade.MetricSourceDto> sources = metricSourceFacade.findAll().stream()
			.filter(MetricSourceFacade.MetricSourceDto::enabled)
			.toList();

		List<PrometheusTargetDto> targets = sources.stream().map(source -> {
			ApplicationAccessFacade.ApplicationDto app =
				applicationAccessFacade.findApplicationById(source.applicationId());
			
			return PrometheusTargetDto.builder()
				.targets(List.of(source.targetHost() + ":" + source.targetPort()))
				.labels(Map.of(
					"__metrics_path__", source.metricsPath(),
					"application_id", app.id().toString(),
					"application_name", app.name()
				))
				.build();
		}).toList();

		return ResponseEntity.ok(targets);
	}
}
