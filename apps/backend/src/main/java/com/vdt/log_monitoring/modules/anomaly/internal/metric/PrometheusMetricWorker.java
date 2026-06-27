package com.vdt.log_monitoring.modules.anomaly.internal.metric;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PrometheusMetricWorker {

    private final PrometheusMetricService prometheusMetricService;

    // Runs every 60 seconds by default
    @Scheduled(fixedRateString = "${anomaly.metric.scrape-interval:60000}")
    public void queryMetrics() {
        log.info("Starting Prometheus metric collection for anomaly detection...");
        
        String cpuUsage = prometheusMetricService.getCpuUsage();
        if (cpuUsage != null) {
            log.info("CPU Usage: {}", cpuUsage);
        }

        String memoryUsage = prometheusMetricService.getMemoryUsage();
        if (memoryUsage != null) {
            log.info("Memory Usage: {}", memoryUsage);
        }

        String diskUsage = prometheusMetricService.getDiskUsage();
        if (diskUsage != null) {
            log.info("Disk Usage: {}", diskUsage);
        }

        String diskWriteRate = prometheusMetricService.getDiskWriteRate();
        if (diskWriteRate != null) {
            log.info("Disk Write Rate (B/s): {}", diskWriteRate);
        }

        String networkReceiveRate = prometheusMetricService.getNetworkReceiveRate();
        if (networkReceiveRate != null) {
            log.info("Network Receive Rate (B/s): {}", networkReceiveRate);
        }

        String networkTransmitRate = prometheusMetricService.getNetworkTransmitRate();
        if (networkTransmitRate != null) {
            log.info("Network Transmit Rate (B/s): {}", networkTransmitRate);
        }
        
        // TODO: In Phase 2, parse responses and update Redis state for anomaly scoring.
    }
}
