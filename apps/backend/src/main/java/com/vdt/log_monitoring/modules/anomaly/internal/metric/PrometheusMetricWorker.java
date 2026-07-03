package com.vdt.log_monitoring.modules.anomaly.internal.metric;

import java.time.Instant;
import java.util.List;
import java.util.OptionalDouble;
import java.util.UUID;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.vdt.log_monitoring.modules.anomaly.internal.rule.AnomalyMetricRule;
import com.vdt.log_monitoring.modules.identity.api.MetricSourceFacade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PrometheusMetricWorker {

    private final PrometheusMetricService prometheusMetricService;
    private final AnomalyMetricRuleHandler anomalyMetricRuleHandler;
    private final MetricSourceFacade metricSourceFacade;

    @Scheduled(fixedRateString = "${app.anomaly.metric.scrape-interval:10s}")
    public void queryMetrics() {
        List<UUID> applicationIds = metricSourceFacade.findAll().stream()
            .filter(MetricSourceFacade.MetricSourceDto::enabled)
            .map(MetricSourceFacade.MetricSourceDto::applicationId)
            .filter(java.util.Objects::nonNull)
            .distinct()
            .toList();
        if (applicationIds.isEmpty()) {
            log.debug("Skipping anomaly metric collection because no enabled metric sources are configured");
            return;
        }
        applicationIds.forEach(this::collectMetrics);
    }

    private void collectMetrics(UUID applicationId) {
        Instant lastSeen = Instant.now();
        saveIfPresent(AnomalyMetricRule.CPU_USAGE, applicationId, prometheusMetricService.getCpuUsage(applicationId), lastSeen);
        saveIfPresent(AnomalyMetricRule.MEMORY_USAGE, applicationId, prometheusMetricService.getMemoryUsage(applicationId), lastSeen);
        saveIfPresent(AnomalyMetricRule.DISK_USAGE, applicationId, prometheusMetricService.getDiskUsage(applicationId), lastSeen);
        saveIfPresent(AnomalyMetricRule.DISK_WRITE_RATE, applicationId, prometheusMetricService.getDiskWriteRate(applicationId), lastSeen);
        saveIfPresent(AnomalyMetricRule.NETWORK_RX_RATE, applicationId, prometheusMetricService.getNetworkReceiveRate(applicationId), lastSeen);
        saveIfPresent(AnomalyMetricRule.NETWORK_TX_RATE, applicationId, prometheusMetricService.getNetworkTransmitRate(applicationId), lastSeen);
    }

    private void saveIfPresent(
        AnomalyMetricRule rule,
        UUID applicationId,
        OptionalDouble current,
        Instant lastSeen) {
        if (current.isPresent()) {
            anomalyMetricRuleHandler.save(AnomalyMetricSnapshot.from(rule, applicationId, current.getAsDouble(), lastSeen));
        }
    }
}
