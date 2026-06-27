package com.vdt.log_monitoring.modules.anomaly.internal.metric;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PrometheusMetricWorker {

    private final RestClient restClient;
    private final String prometheusUrl;

    public PrometheusMetricWorker(
            RestClient.Builder restClientBuilder,
            @Value("${prometheus.api-url:http://localhost:9090}") String prometheusUrl) {
        this.restClient = restClientBuilder.build();
        this.prometheusUrl = prometheusUrl;
    }

    // Runs every 60 seconds by default
    @Scheduled(fixedRateString = "${anomaly.metric.scrape-interval:60000}")
    public void queryMetrics() {
        log.info("Starting Prometheus metric collection for anomaly detection...");
        
        try {
            // Example: Querying CPU usage. In phase 2 this will query specific targets.
            String queryUrl = prometheusUrl + "/api/v1/query?query=sum(rate(container_cpu_usage_seconds_total[1m])) by (container_label_com_docker_compose_service)";
            
            String response = restClient.get()
                    .uri(queryUrl)
                    .retrieve()
                    .body(String.class);
            
            log.info("Successfully fetched metrics from Prometheus: {}", response);
            
            // TODO: In Phase 2, parse response and update Redis state for each application metric.
        } catch (RestClientException e) {
            log.error("Failed to fetch metrics from Prometheus", e);
        }
    }
}
