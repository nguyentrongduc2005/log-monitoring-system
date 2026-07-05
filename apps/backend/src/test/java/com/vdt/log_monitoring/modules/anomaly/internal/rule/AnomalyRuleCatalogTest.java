package com.vdt.log_monitoring.modules.anomaly.internal.rule;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

class AnomalyRuleCatalogTest {

	@Test
	void logRulesUseConfiguredThresholds() {
		assertThat(AnomalyLogRule.RESOURCE_EXHAUSTED.thresholdCount()).isEqualTo(1);
		assertThat(AnomalyLogRule.SECURITY_AUTH_FAILURE.thresholdCount()).isEqualTo(5);
		assertThat(AnomalyLogRule.ERROR_LEVEL.thresholdCount()).isEqualTo(15);
	}

	@Test
	void metricRulesUseConfiguredThresholds() {
		assertThat(AnomalyMetricRule.CPU_USAGE.warningThreshold()).isEqualTo(85.0);
		assertThat(AnomalyMetricRule.DISK_WRITE_RATE.warningThreshold()).isEqualTo(50_000_000.0);
	}

	@Test
	void metricRulesIgnoreSingleSampleSpikes() {
		assertThat(AnomalyMetricRule.CPU_USAGE.isBreached(List.of(20.0, 22.0, 91.0, 23.0, 21.0, 20.0)))
			.isFalse();
	}

	@Test
	void metricRulesBreachWarningWhenFourOfSixSamplesCrossWarningThreshold() {
		List<Double> recentValues = List.of(20.0, 88.0, 89.0, 87.0, 84.0, 86.0);

		assertThat(AnomalyMetricRule.CPU_USAGE.isBreached(recentValues)).isTrue();
		assertThat(AnomalyMetricRule.CPU_USAGE.severityFor(recentValues)).isEqualTo("ERROR");
	}

	@Test
	void metricRulesBreachCriticalWhenTwoOfLastThreeSamplesCrossCriticalThreshold() {
		List<Double> recentValues = List.of(80.0, 86.0, 88.0, 91.0, 92.0, 89.0);

		assertThat(AnomalyMetricRule.CPU_USAGE.isBreached(recentValues)).isTrue();
		assertThat(AnomalyMetricRule.CPU_USAGE.severityFor(recentValues)).isEqualTo("CRITICAL");
	}

	@Test
	void logRuleTtlIsWindowPlusGrace() {
		assertThat(AnomalyLogRule.TIMEOUT.ttl())
			.isEqualTo(AnomalyLogRule.TIMEOUT.window().plus(Duration.ofSeconds(60)));
	}
}
