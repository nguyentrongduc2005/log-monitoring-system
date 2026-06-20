package com.vdt.log_monitoring.modules.alerting.internal.detection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.vdt.log_monitoring.modules.alerting.api.AlertingFacade;
import com.vdt.log_monitoring.modules.processing.api.events.CriticalLogDetectedEvent;

class AlertDetectionServiceTest {

	@Test
	void detectMapsCriticalLogEventToAlertCandidate() {
		AlertingFacade alertingFacade = mock();
		AlertDetectionService service = new AlertDetectionService(alertingFacade);
		CriticalLogDetectedEvent event = event("trace-1");

		service.detect(event);

		ArgumentCaptor<AlertingFacade.AlertCandidate> captor =
			ArgumentCaptor.forClass(AlertingFacade.AlertCandidate.class);
		verify(alertingFacade).evaluate(captor.capture());
		AlertingFacade.AlertCandidate candidate = captor.getValue();
		assertThat(candidate.eventId()).isEqualTo(event.eventId());
		assertThat(candidate.applicationId()).isEqualTo(event.applicationId());
		assertThat(candidate.severity()).isEqualTo(event.level());
		assertThat(candidate.message()).isEqualTo(event.message());
		assertThat(candidate.fingerprint()).isEqualTo(event.fingerprint());
	}

	@Test
	void detectAcceptsLegacyEventWithoutTraceId() {
		AlertingFacade alertingFacade = mock();
		AlertDetectionService service = new AlertDetectionService(alertingFacade);
		CriticalLogDetectedEvent event = event(null);

		service.detect(event);

		verify(alertingFacade).evaluate(any());
	}

	private CriticalLogDetectedEvent event(String traceId) {
		return new CriticalLogDetectedEvent(
			UUID.fromString("00000000-0000-0000-0000-000000000101"),
			UUID.fromString("00000000-0000-0000-0000-000000000102"),
			UUID.fromString("00000000-0000-0000-0000-000000000103"),
			"payments",
			"Payments",
			traceId,
			"CRITICAL",
			"Payment failed",
			"payment-failed",
			Instant.parse("2026-06-20T10:00:00Z"),
			Instant.parse("2026-06-20T10:00:01Z"),
			CriticalLogDetectedEvent.CURRENT_SCHEMA_VERSION
		);
	}
}
