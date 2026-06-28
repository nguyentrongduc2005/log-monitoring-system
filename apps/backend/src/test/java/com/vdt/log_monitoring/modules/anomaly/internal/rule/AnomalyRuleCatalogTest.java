package com.vdt.log_monitoring.modules.anomaly.internal.rule;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

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
	void logRuleTtlIsWindowPlusGrace() {
		assertThat(AnomalyLogRule.TIMEOUT.ttl())
			.isEqualTo(AnomalyLogRule.TIMEOUT.window().plus(Duration.ofSeconds(60)));
	}
}
