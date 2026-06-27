package com.vdt.log_monitoring.modules.anomaly.internal.metric;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PrometheusMetricService {

    private final RestClient restClient;
    private final String prometheusUrl;

    public PrometheusMetricService(
            RestClient.Builder restClientBuilder,
            @Value("${prometheus.api-url:http://localhost:9090}") String prometheusUrl) {
        this.restClient = restClientBuilder.build();
        this.prometheusUrl = prometheusUrl;
    }

    /**
     * Query Node Exporter CPU Usage (%) over the last 1 minute.
     * Formula: 100 - (avg by (instance) (rate(node_cpu_seconds_total{mode="idle"}[1m])) * 100)
     */
    public String getCpuUsage() {
        String query = "100 - (avg by (instance) (rate(node_cpu_seconds_total{mode=\"idle\"}[1m])) * 100)";
        return executeQuery(query, "CPU Usage");
    }

    /**
     * Query Node Exporter Memory Usage (%).
     * Formula: (node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes) / node_memory_MemTotal_bytes * 100
     */
    public String getMemoryUsage() {
        String query = "(node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes) / node_memory_MemTotal_bytes * 100";
        return executeQuery(query, "Memory Usage");
    }

    /**
     * Query Node Exporter Disk Space Usage (%) for root filesystem.
     */
    public String getDiskUsage() {
        String query = "100 - ((node_filesystem_avail_bytes{mountpoint=\"/\",fstype!=\"rootfs\"} * 100) / node_filesystem_size_bytes{mountpoint=\"/\",fstype!=\"rootfs\"})";
        return executeQuery(query, "Disk Usage");
    }

    private String executeQuery(String query, String metricName) {
        try {
            String queryUrl = prometheusUrl + "/api/v1/query?query=" + query;
            return restClient.get()
                    .uri(queryUrl)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            log.error("Failed to fetch {} from Prometheus", metricName, e);
            return null;
        }
    }
}
