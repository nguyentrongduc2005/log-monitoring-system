package com.vdt.log_monitoring.modules.processing.internal.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.vdt.log_monitoring.modules.alerting.api.AlertingFacade;
import com.vdt.log_monitoring.modules.processing.internal.model.LogFingerprint;
import com.vdt.log_monitoring.modules.processing.internal.model.LogLevel;
import com.vdt.log_monitoring.modules.processing.internal.model.LogMetadata;
import com.vdt.log_monitoring.modules.processing.internal.model.LogProcessingStatus;
import com.vdt.log_monitoring.modules.processing.internal.model.ProcessedLog;

class AlertCandidateRuleFilterTest {

	private static final UUID APPLICATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");

	private final AlertingFacade alertingFacade = org.mockito.Mockito.mock();
	private final AlertCandidateRuleFilter filter = new AlertCandidateRuleFilter(alertingFacade);

	@Test
	void matchesWarnLogWhenAlertingFacadeFindsMatchingRule() {
		Instant timestamp = Instant.parse("2026-06-18T03:00:00Z");
		when(alertingFacade.hasMatchingActiveRuleCandidate(
			APPLICATION_ID,
			"WARN",
			"payment_failed from gateway",
			timestamp))
			.thenReturn(true);

		assertThat(filter.matches(log(LogLevel.WARN, "payment_failed from gateway", timestamp)))
			.isTrue();
	}

	@Test
	void returnsFalseWhenAlertingFacadeFindsNoMatchingRule() {
		Instant timestamp = Instant.parse("2026-06-18T03:00:00Z");
		when(alertingFacade.hasMatchingActiveRuleCandidate(
			APPLICATION_ID,
			"INFO",
			"payment_failed from gateway",
			timestamp))
			.thenReturn(false);

		assertThat(filter.matches(log(LogLevel.INFO, "payment_failed from gateway", timestamp)))
			.isFalse();
	}

	@Test
	void fallsBackToCriticalLevelsWithoutReadingRules() {
		assertThat(filter.matches(log(LogLevel.ERROR, "payment_failed from gateway", Instant.parse("2026-06-18T03:00:00Z"))))
			.isTrue();
		verify(alertingFacade, never()).hasMatchingActiveRuleCandidate(
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any());
	}

	private ProcessedLog log(LogLevel level, String message, Instant instant) {
		return new ProcessedLog(
			UUID.randomUUID(),
			UUID.randomUUID(),
			APPLICATION_ID,
			"checkout-api",
			"Checkout API",
			level,
			message,
			null,
			instant,
			instant,
			instant,
			new LogFingerprint("checkout-payment"),
			LogProcessingStatus.STORED,
			LogMetadata.empty()
		);
	}
}
