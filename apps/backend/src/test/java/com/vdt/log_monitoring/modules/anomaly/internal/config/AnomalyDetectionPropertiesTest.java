package com.vdt.log_monitoring.modules.anomaly.internal.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class AnomalyDetectionPropertiesTest {

	@Test
	void appliesRuntimeDefaults() {
		AnomalyDetectionProperties properties = new AnomalyDetectionProperties(null, null);

		assertThat(properties.indexTtl()).isEqualTo(Duration.ofMinutes(15));
		assertThat(properties.metric().scrapeInterval()).isEqualTo(Duration.ofSeconds(10));
		assertThat(properties.metric().freshnessTtl()).isEqualTo(Duration.ofSeconds(60));
		assertThat(properties.metric().effectiveFreshnessTtl()).isEqualTo(Duration.ofSeconds(60));
	}

	@Test
	void keepsMetricSnapshotsFreshForAtLeastSixScrapeIntervals() {
		AnomalyDetectionProperties.Metric metric =
			new AnomalyDetectionProperties.Metric(Duration.ofSeconds(30), Duration.ofSeconds(45));

		assertThat(metric.effectiveFreshnessTtl()).isEqualTo(Duration.ofSeconds(180));
	}
}
