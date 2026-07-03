package com.vdt.log_monitoring.modules.anomaly.internal.log;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.vdt.log_monitoring.modules.anomaly.internal.rule.AnomalyLogRule;
import com.vdt.log_monitoring.modules.processing.api.events.AnomalySignalEvent;

class AnomalyLogClassifierTest {

	private static final UUID APP_ID = UUID.fromString("00000000-0000-0000-0000-000000000103");
	private static final UUID LOG_ID = UUID.fromString("00000000-0000-0000-0000-000000000104");
	private static final Instant TIMESTAMP = Instant.parse("2026-06-28T00:00:00Z");

	private final AnomalyLogClassifier classifier = new AnomalyLogClassifier();

	@Test
	void classifiesFailedLoginByUser() {
		AnomalyLogMatch match = classify("ERROR", "failed login for user=john from ip=10.0.0.8");

		assertThat(match.rule()).isEqualTo(AnomalyLogRule.SECURITY_AUTH_FAILURE);
		assertThat(match.dimensionType()).isEqualTo("user");
		assertThat(match.dimensionValue()).isEqualTo("john");
	}

	@Test
	void classifiesSshFailedPasswordByIp() {
		AnomalyLogMatch match = classify("WARN", "ssh failed password from ip=10.0.0.8");

		assertThat(match.rule()).isEqualTo(AnomalyLogRule.SECURITY_AUTH_FAILURE);
		assertThat(match.dimensionType()).isEqualTo("ip");
		assertThat(match.dimensionValue()).isEqualTo("10.0.0.8");
	}

	@Test
	void resourceExhaustedWinsBeforeGenericError() {
		AnomalyLogMatch match = classify("ERROR", "out of memory while handling request");

		assertThat(match.rule()).isEqualTo(AnomalyLogRule.RESOURCE_EXHAUSTED);
	}

	@Test
	void classifiesTimeoutExceptionAndSuspiciousKeywords() {
		assertThat(classify("WARN", "connection timeout to downstream").rule()).isEqualTo(AnomalyLogRule.TIMEOUT);
		assertThat(classify("ERROR", "NullPointerException at service").rule()).isEqualTo(AnomalyLogRule.EXCEPTION);
		assertThat(classify("INFO", "payment failed after provider response").rule()).isEqualTo(AnomalyLogRule.SUSPICIOUS_KEYWORD);
	}

	@Test
	void fallsBackToLevelRulesAfterSpecificRules() {
		assertThat(classify("CRITICAL", "generic severe message").rule()).isEqualTo(AnomalyLogRule.CRITICAL_LEVEL);
		assertThat(classify("ERROR", "generic error message").rule()).isEqualTo(AnomalyLogRule.ERROR_LEVEL);
	}

	@Test
	void ignoresInfoLogsWithoutSuspiciousKeywords() {
		Optional<AnomalyLogMatch> match = classifier.classify(event("INFO", "normal processing completed"));

		assertThat(match).isEmpty();
	}

	private AnomalyLogMatch classify(String level, String message) {
		return classifier.classify(event(level, message)).orElseThrow();
	}

	private AnomalySignalEvent event(String level, String message) {
		return new AnomalySignalEvent(
			APP_ID,
			"checkout-api",
			"Checkout API",
			TIMESTAMP,
			LOG_ID,
			level,
			"TEST",
			"payments",
			message,
			"fingerprint-1",
			"trace-1",
			java.util.Map.of()
		);
	}
}
