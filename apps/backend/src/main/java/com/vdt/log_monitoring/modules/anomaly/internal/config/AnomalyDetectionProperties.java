package com.vdt.log_monitoring.modules.anomaly.internal.config;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.anomaly")
public record AnomalyDetectionProperties(
	Duration indexTtl,
	Metric metric,
	Collector collector,
	Scoring scoring
) {

	public AnomalyDetectionProperties {
		indexTtl = indexTtl == null ? Duration.ofMinutes(15) : indexTtl;
		metric = metric == null ? new Metric(Duration.ofSeconds(10), Duration.ofSeconds(60), null) : metric;
		collector = collector == null ? new Collector(null, null) : collector;
		scoring = scoring == null ? new Scoring(0, 0, null, null) : scoring;
	}

	public record Metric(
		Duration scrapeInterval,
		Duration freshnessTtl,
		String defaultApplicationId
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

		public Optional<UUID> resolvedDefaultApplicationId() {
			if (defaultApplicationId == null || defaultApplicationId.isBlank()) {
				return Optional.empty();
			}
			return Optional.of(UUID.fromString(defaultApplicationId.trim()));
		}
	}

	public record Collector(
		Boolean enabled,
		Duration interval
	) {

		public Collector {
			enabled = enabled == null ? Boolean.TRUE : enabled;
			interval = interval == null ? Duration.ofSeconds(30) : interval;
		}
	}

	public record Scoring(
		int metricConfidenceSamples,
		int topEvidenceLimit,
		Thresholds thresholds,
		Map<String, Double> weights
	) {

		public Scoring {
			metricConfidenceSamples = metricConfidenceSamples <= 0 ? 3 : metricConfidenceSamples;
			topEvidenceLimit = topEvidenceLimit <= 0 ? 5 : topEvidenceLimit;
			thresholds = thresholds == null ? new Thresholds(40, 70, 85) : thresholds;
			weights = weights == null || weights.isEmpty() ? defaultWeights() : Map.copyOf(weights);
		}

		public double weightFor(String key) {
			return weights.getOrDefault(key, 0.0);
		}

		private static Map<String, Double> defaultWeights() {
			return Map.ofEntries(
				Map.entry("log.RESOURCE_EXHAUSTED", 0.12),
				Map.entry("log.SECURITY_AUTH_FAILURE", 0.18),
				Map.entry("log.ACCESS_DENIED", 0.10),
				Map.entry("log.TIMEOUT", 0.10),
				Map.entry("log.EXCEPTION", 0.10),
				Map.entry("log.CRITICAL_LEVEL", 0.12),
				Map.entry("log.ERROR_LEVEL", 0.10),
				Map.entry("log.SUSPICIOUS_KEYWORD", 0.08),
				Map.entry("metric.CPU_USAGE", 0.08),
				Map.entry("metric.MEMORY_USAGE", 0.07),
				Map.entry("metric.DISK_USAGE", 0.04),
				Map.entry("metric.DISK_WRITE_RATE", 0.00),
				Map.entry("metric.NETWORK_RX_RATE", 0.00),
				Map.entry("metric.NETWORK_TX_RATE", 0.00));
		}
	}

	public record Thresholds(
		int warning,
		int anomaly,
		int critical
	) {

		public Thresholds {
			warning = warning <= 0 ? 40 : warning;
			anomaly = anomaly <= warning ? 70 : anomaly;
			critical = critical <= anomaly ? 85 : critical;
		}
	}
}
