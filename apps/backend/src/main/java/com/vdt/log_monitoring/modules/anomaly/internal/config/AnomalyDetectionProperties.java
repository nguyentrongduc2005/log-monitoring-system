package com.vdt.log_monitoring.modules.anomaly.internal.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.anomaly")
public record AnomalyDetectionProperties(
	Duration indexTtl,
	Metric metric
) {

	public AnomalyDetectionProperties {
		indexTtl = indexTtl == null ? Duration.ofMinutes(15) : indexTtl;
		metric = metric == null ? new Metric(Duration.ofSeconds(10), Duration.ofSeconds(60)) : metric;
	}

	public record Metric(
		Duration scrapeInterval,
		Duration freshnessTtl
	) {

		public Metric {
			scrapeInterval = scrapeInterval == null ? Duration.ofSeconds(10) : scrapeInterval;
			freshnessTtl = freshnessTtl == null ? Duration.ofSeconds(60) : freshnessTtl;
		}

		public Duration effectiveFreshnessTtl() {
			Duration minimumFromScrape = scrapeInterval.multipliedBy(6);
			Duration minimumFreshness = Duration.ofSeconds(60);
			return List.of(freshnessTtl, minimumFromScrape, minimumFreshness).stream()
				.max(Duration::compareTo)
				.orElse(minimumFreshness);
		}
	}
}
