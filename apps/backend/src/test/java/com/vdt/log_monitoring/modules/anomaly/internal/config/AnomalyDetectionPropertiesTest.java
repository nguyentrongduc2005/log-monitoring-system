package com.vdt.log_monitoring.modules.anomaly.internal.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class AnomalyDetectionPropertiesTest {

	@Test
	void appliesCollectorAndScoringDefaults() {
		AnomalyDetectionProperties properties = new AnomalyDetectionProperties(null, null, null, null);

		assertThat(properties.collector().enabled()).isTrue();
		assertThat(properties.collector().interval()).isEqualTo(Duration.ofSeconds(30));
		assertThat(properties.scoring().metricConfidenceSamples()).isEqualTo(3);
		assertThat(properties.scoring().topEvidenceLimit()).isEqualTo(5);
		assertThat(properties.scoring().thresholds().warning()).isEqualTo(40);
		assertThat(properties.scoring().thresholds().anomaly()).isEqualTo(70);
		assertThat(properties.scoring().thresholds().critical()).isEqualTo(85);
		assertThat(properties.scoring().weightFor("log.SECURITY_AUTH_FAILURE")).isEqualTo(0.18);
		assertThat(properties.scoring().weightFor("log.ERROR_LEVEL")).isEqualTo(0.10);
		assertThat(properties.scoring().weightFor("metric.CPU_USAGE")).isEqualTo(0.08);
		assertThat(properties.scoring().weightFor("metric.MEMORY_USAGE")).isEqualTo(0.07);
		assertThat(properties.scoring().weightFor("unknown.rule")).isZero();
	}
}
