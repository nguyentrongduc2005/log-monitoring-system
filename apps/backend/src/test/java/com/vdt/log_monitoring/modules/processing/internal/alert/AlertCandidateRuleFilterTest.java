package com.vdt.log_monitoring.modules.processing.internal.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
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
	void matchesWarnLogWhenActiveRuleCandidateMatchesSeverityAndKeyword() {
		when(alertingFacade.findActiveRuleCandidates(APPLICATION_ID))
				.thenReturn(List.of(new AlertingFacade.ActiveAlertRuleCandidateDto(
						"WARN",
						"payment_failed")));

		assertThat(filter.matches(log(LogLevel.WARN, "payment_failed from gateway", "2026-06-18T03:00:00Z")))
				.isTrue();
		assertThat(filter.matches(log(LogLevel.WARN, "payment_failed from gateway", "2026-06-18T10:00:00Z")))
				.isTrue();
	}

	@Test
	void fallsBackToCriticalLevelsWhenNoActiveRuleCandidatesExist() {
		when(alertingFacade.findActiveRuleCandidates(APPLICATION_ID)).thenReturn(List.of());

		assertThat(filter.matches(log(LogLevel.INFO, "payment_failed from gateway", "2026-06-18T03:00:00Z")))
				.isFalse();
		assertThat(filter.matches(log(LogLevel.ERROR, "payment_failed from gateway", "2026-06-18T03:00:00Z")))
				.isTrue();
	}

	private ProcessedLog log(LogLevel level, String message, String timestamp) {
		Instant instant = Instant.parse(timestamp);
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
				LogMetadata.empty());
	}
}
