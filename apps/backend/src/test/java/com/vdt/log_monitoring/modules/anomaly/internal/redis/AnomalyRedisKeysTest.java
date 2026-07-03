package com.vdt.log_monitoring.modules.anomaly.internal.redis;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.vdt.log_monitoring.modules.anomaly.internal.rule.AnomalyLogRule;
import com.vdt.log_monitoring.modules.anomaly.internal.rule.AnomalyMetricRule;

class AnomalyRedisKeysTest {

	private static final UUID APP_ID = UUID.fromString("00000000-0000-0000-0000-000000000103");

	private final AnomalyRedisKeys keys = new AnomalyRedisKeys();

	@Test
	void buildsApplicationScopedLogCounterKey() {
		assertThat(keys.logCounter(APP_ID, AnomalyLogRule.SECURITY_AUTH_FAILURE, "ip", "10.0.0.8"))
			.isEqualTo("anomaly:%s:log:SECURITY_AUTH_FAILURE:ip:10.0.0.8".formatted(APP_ID));
	}

	@Test
	void sanitizesKeyParts() {
		assertThat(keys.logCounter(APP_ID, AnomalyLogRule.TIMEOUT, "service name", "payment:worker"))
			.isEqualTo("anomaly:%s:log:TIMEOUT:service_name:payment_worker".formatted(APP_ID));
	}

	@Test
	void buildsRuleScopedLogIndexKeyAndDimensionMember() {
		assertThat(keys.logRuleActiveIndex(APP_ID, AnomalyLogRule.TIMEOUT))
			.isEqualTo("anomaly:%s:log:TIMEOUT:active".formatted(APP_ID));
		assertThat(keys.logDimensionMember("service", "payment")).isEqualTo("service:payment");
	}

	@Test
	void buildsMetricKeys() {
		assertThat(keys.metricSnapshot(APP_ID, AnomalyMetricRule.CPU_USAGE))
			.isEqualTo("anomaly:%s:metric:CPU_USAGE".formatted(APP_ID));
		assertThat(keys.activeMetricRules(APP_ID))
			.isEqualTo("anomaly:%s:metric-rules:active".formatted(APP_ID));
	}
}
