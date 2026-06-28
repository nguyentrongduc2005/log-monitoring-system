package com.vdt.log_monitoring.modules.anomaly.internal.metric;

import java.net.URI;
import java.util.OptionalDouble;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PrometheusMetricService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String prometheusUrl;

    public PrometheusMetricService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${prometheus.api-url:http://localhost:9090}") String prometheusUrl) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.prometheusUrl = prometheusUrl;
    }

    public OptionalDouble getCpuUsage(UUID applicationId) {
        String query = "100 - (avg by (application_id) (rate(node_cpu_seconds_total{%s,mode=\"idle\"}[1m])) * 100)"
                .formatted(applicationSelector(applicationId));
        return executeQuery(query, "CPU Usage");
    }

    public OptionalDouble getMemoryUsage(UUID applicationId) {
        String selector = applicationSelector(applicationId);
        String query = "(node_memory_MemTotal_bytes{%s} - node_memory_MemAvailable_bytes{%s}) / node_memory_MemTotal_bytes{%s} * 100"
                .formatted(selector, selector, selector);
        return executeQuery(query, "Memory Usage");
    }

    public OptionalDouble getDiskUsage(UUID applicationId) {
        String selector = applicationSelector(applicationId) + ",mountpoint=\"/\",fstype!=\"rootfs\"";
        String query = "100 - ((node_filesystem_avail_bytes{%s} * 100) / node_filesystem_size_bytes{%s})"
                .formatted(selector, selector);
        return executeQuery(query, "Disk Usage");
    }

    public OptionalDouble getDiskWriteRate(UUID applicationId) {
        String query = "sum by (application_id) (rate(node_disk_written_bytes_total{%s}[1m]))"
                .formatted(applicationSelector(applicationId));
        return executeQuery(query, "Disk Write Rate");
    }

    public OptionalDouble getNetworkReceiveRate(UUID applicationId) {
        String query = "sum by (application_id) (rate(node_network_receive_bytes_total{%s}[1m]))"
                .formatted(applicationSelector(applicationId));
        return executeQuery(query, "Network Receive Rate");
    }

    public OptionalDouble getNetworkTransmitRate(UUID applicationId) {
        String query = "sum by (application_id) (rate(node_network_transmit_bytes_total{%s}[1m]))"
                .formatted(applicationSelector(applicationId));
        return executeQuery(query, "Network Transmit Rate");
    }

    private OptionalDouble executeQuery(String query, String metricName) {
        try {
            String body = restClient.get()
                    .uri(prometheusQueryUri(query))
                    .retrieve()
                    .body(String.class);
            return parseFirstValue(body);
        } catch (RestClientException | IllegalArgumentException e) {
            log.error("Failed to fetch {} from Prometheus", metricName, e);
            return OptionalDouble.empty();
        }
    }

    private URI prometheusQueryUri(String query) {
        return UriComponentsBuilder.fromUriString(prometheusUrl)
                .path("/api/v1/query")
                .queryParam("query", query)
                .build()
                .encode()
                .toUri();
    }

    private String applicationSelector(UUID applicationId) {
        if (applicationId == null) {
            throw new IllegalArgumentException("applicationId must not be null");
        }
        return "application_id=\"%s\"".formatted(applicationId);
    }

    private OptionalDouble parseFirstValue(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            if (!"success".equals(root.path("status").asText())) {
                return OptionalDouble.empty();
            }
            JsonNode value = root.path("data").path("result").path(0).path("value").path(1);
            if (!value.isTextual()) {
                return OptionalDouble.empty();
            }
            return OptionalDouble.of(Double.parseDouble(value.asText()));
        } catch (Exception e) {
            log.debug("Failed to parse Prometheus response", e);
            return OptionalDouble.empty();
        }
    }
}
