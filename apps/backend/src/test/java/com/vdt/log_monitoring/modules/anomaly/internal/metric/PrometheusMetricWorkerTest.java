package com.vdt.log_monitoring.modules.anomaly.internal.metric;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.OptionalDouble;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.vdt.log_monitoring.modules.anomaly.internal.rule.AnomalyMetricRule;
import com.vdt.log_monitoring.modules.identity.api.MetricSourceFacade;

class PrometheusMetricWorkerTest {

	private static final UUID APP_ID = UUID.fromString("00000000-0000-0000-0000-000000000103");
	private static final UUID SECOND_APP_ID = UUID.fromString("00000000-0000-0000-0000-000000000104");

	private final PrometheusMetricService prometheusMetricService = org.mockito.Mockito.mock();
	private final AnomalyMetricRuleHandler anomalyMetricRuleHandler = org.mockito.Mockito.mock();
	private final MetricSourceFacade metricSourceFacade = org.mockito.Mockito.mock();

	@Test
	void savesSnapshotForPresentMetricValue() {
		when(metricSourceFacade.findAll()).thenReturn(List.of(metricSource(APP_ID, true)));
		when(prometheusMetricService.getCpuUsage(APP_ID)).thenReturn(OptionalDouble.of(91.2));
		when(prometheusMetricService.getMemoryUsage(APP_ID)).thenReturn(OptionalDouble.empty());
		when(prometheusMetricService.getDiskUsage(APP_ID)).thenReturn(OptionalDouble.empty());
		when(prometheusMetricService.getDiskWriteRate(APP_ID)).thenReturn(OptionalDouble.empty());
		when(prometheusMetricService.getNetworkReceiveRate(APP_ID)).thenReturn(OptionalDouble.empty());
		when(prometheusMetricService.getNetworkTransmitRate(APP_ID)).thenReturn(OptionalDouble.empty());
		PrometheusMetricWorker worker = newWorker();

		worker.queryMetrics();

		ArgumentCaptor<AnomalyMetricSnapshot> snapshotCaptor = ArgumentCaptor.forClass(AnomalyMetricSnapshot.class);
		verify(anomalyMetricRuleHandler).save(snapshotCaptor.capture());
		assertThat(snapshotCaptor.getValue().rule()).isEqualTo(AnomalyMetricRule.CPU_USAGE);
		assertThat(snapshotCaptor.getValue().applicationId()).isEqualTo(APP_ID);
		assertThat(snapshotCaptor.getValue().current()).isEqualTo(91.2);
	}

	@Test
	void skipsMetricWhenValueIsMissing() {
		when(metricSourceFacade.findAll()).thenReturn(List.of(metricSource(APP_ID, true)));
		when(prometheusMetricService.getCpuUsage(APP_ID)).thenReturn(OptionalDouble.empty());
		when(prometheusMetricService.getMemoryUsage(APP_ID)).thenReturn(OptionalDouble.empty());
		when(prometheusMetricService.getDiskUsage(APP_ID)).thenReturn(OptionalDouble.empty());
		when(prometheusMetricService.getDiskWriteRate(APP_ID)).thenReturn(OptionalDouble.empty());
		when(prometheusMetricService.getNetworkReceiveRate(APP_ID)).thenReturn(OptionalDouble.empty());
		when(prometheusMetricService.getNetworkTransmitRate(APP_ID)).thenReturn(OptionalDouble.empty());
		PrometheusMetricWorker worker = newWorker();

		worker.queryMetrics();

		verify(anomalyMetricRuleHandler, never()).save(org.mockito.Mockito.any());
	}

	@Test
	void skipsCollectionWhenNoEnabledMetricSourcesExist() {
		when(metricSourceFacade.findAll()).thenReturn(List.of(metricSource(APP_ID, false)));
		PrometheusMetricWorker worker = newWorker();

		worker.queryMetrics();

		verify(prometheusMetricService, never()).getCpuUsage(org.mockito.Mockito.any());
		verify(anomalyMetricRuleHandler, never()).save(org.mockito.Mockito.any());
	}

	@Test
	void collectsMetricsForEachEnabledMetricSource() {
		when(metricSourceFacade.findAll()).thenReturn(List.of(
			metricSource(APP_ID, true),
			metricSource(SECOND_APP_ID, true),
			metricSource(UUID.fromString("00000000-0000-0000-0000-000000000105"), false)));
		when(prometheusMetricService.getCpuUsage(APP_ID)).thenReturn(OptionalDouble.of(91.2));
		when(prometheusMetricService.getCpuUsage(SECOND_APP_ID)).thenReturn(OptionalDouble.of(73.4));
		when(prometheusMetricService.getMemoryUsage(org.mockito.Mockito.any())).thenReturn(OptionalDouble.empty());
		when(prometheusMetricService.getDiskUsage(org.mockito.Mockito.any())).thenReturn(OptionalDouble.empty());
		when(prometheusMetricService.getDiskWriteRate(org.mockito.Mockito.any())).thenReturn(OptionalDouble.empty());
		when(prometheusMetricService.getNetworkReceiveRate(org.mockito.Mockito.any())).thenReturn(OptionalDouble.empty());
		when(prometheusMetricService.getNetworkTransmitRate(org.mockito.Mockito.any())).thenReturn(OptionalDouble.empty());
		PrometheusMetricWorker worker = newWorker();

		worker.queryMetrics();

		ArgumentCaptor<AnomalyMetricSnapshot> snapshotCaptor = ArgumentCaptor.forClass(AnomalyMetricSnapshot.class);
		verify(anomalyMetricRuleHandler, times(2)).save(snapshotCaptor.capture());
		assertThat(snapshotCaptor.getAllValues())
			.extracting(AnomalyMetricSnapshot::applicationId)
			.containsExactlyInAnyOrder(APP_ID, SECOND_APP_ID);
	}

	private PrometheusMetricWorker newWorker() {
		return new PrometheusMetricWorker(
			prometheusMetricService,
			anomalyMetricRuleHandler,
			metricSourceFacade);
	}

	private MetricSourceFacade.MetricSourceDto metricSource(UUID applicationId, boolean enabled) {
		Instant now = Instant.parse("2026-06-24T08:00:00Z");
		return new MetricSourceFacade.MetricSourceDto(
			UUID.randomUUID(),
			applicationId,
			"localhost",
			9100,
			"/metrics",
			"10s",
			enabled,
			now,
			now);
	}
}
