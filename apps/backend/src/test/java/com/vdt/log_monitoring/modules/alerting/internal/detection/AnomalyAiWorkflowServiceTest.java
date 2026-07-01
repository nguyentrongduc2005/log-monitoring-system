package com.vdt.log_monitoring.modules.alerting.internal.detection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vdt.log_monitoring.modules.anomaly.api.AnomalyFacade;
import com.vdt.log_monitoring.modules.anomaly.api.events.AnomalyDetectedEvent;
import com.vdt.log_monitoring.modules.incident.api.IncidentFacade;
import com.vdt.log_monitoring.modules.realtime.api.RealtimeFacade;
import com.vdt.log_monitoring.modules.realtime.api.events.AnomalyReportNotificationMessage;

@ExtendWith(MockitoExtension.class)
class AnomalyAiWorkflowServiceTest {

	private static final UUID REPORT_ID = UUID.fromString("00000000-0000-0000-0000-000000000301");
	private static final UUID ALERT_ID = UUID.fromString("00000000-0000-0000-0000-000000000401");
	private static final UUID APP_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
	private static final Instant NOW = Instant.parse("2026-06-29T00:00:00Z");

	@Mock
	private AnomalyFacade anomalyFacade;
	@Mock
	private IncidentFacade incidentFacade;
	@Mock
	private RealtimeFacade realtimeFacade;

	@Test
	void notifiesWhenAiStartsAndCompletes() {
		AnomalyAiWorkflowService service = service();
		AnomalyDetectedEvent event = event();
		when(anomalyFacade.findReportById(REPORT_ID)).thenReturn(report("SUCCEEDED", NOW.plusSeconds(5), null));

		service.requestAnalysis(event, ALERT_ID);

		verify(anomalyFacade).markAiPending(REPORT_ID, "RULE_REQUIRES_AI");
		verify(incidentFacade).requestAnomalyReportAi(REPORT_ID, ALERT_ID, "RULE_REQUIRES_AI");

		ArgumentCaptor<AnomalyReportNotificationMessage> captor =
			ArgumentCaptor.forClass(AnomalyReportNotificationMessage.class);
		verify(realtimeFacade, org.mockito.Mockito.times(2)).publishAnomalyReportNotification(captor.capture());
		assertThat(captor.getAllValues())
			.extracting(AnomalyReportNotificationMessage::updateType)
			.containsExactly("AI_STARTED", "AI_COMPLETED");
		assertThat(captor.getAllValues().get(1).aiStatus()).isEqualTo("SUCCEEDED");
	}

	@Test
	void persistsFailureAndNotifiesWhenAiRequestThrows() {
		AnomalyAiWorkflowService service = service();
		AnomalyDetectedEvent event = event();
		doThrow(new IllegalStateException("AI unavailable"))
			.when(incidentFacade)
			.requestAnomalyReportAi(REPORT_ID, ALERT_ID, "RULE_REQUIRES_AI");
		when(anomalyFacade.findReportById(REPORT_ID)).thenReturn(report("FAILED", NOW.plusSeconds(5), "AI unavailable"));

		service.requestAnalysis(event, ALERT_ID);

		verify(anomalyFacade).updateAiFailure(eq(REPORT_ID), eq("AI unavailable"));
		ArgumentCaptor<AnomalyReportNotificationMessage> captor =
			ArgumentCaptor.forClass(AnomalyReportNotificationMessage.class);
		verify(realtimeFacade, org.mockito.Mockito.times(2)).publishAnomalyReportNotification(captor.capture());
		assertThat(captor.getAllValues())
			.extracting(AnomalyReportNotificationMessage::updateType)
			.containsExactly("AI_STARTED", "AI_FAILED");
		assertThat(captor.getAllValues().get(1).aiStatus()).isEqualTo("FAILED");
	}

	private AnomalyAiWorkflowService service() {
		return new AnomalyAiWorkflowService(
			anomalyFacade,
			incidentFacade,
			realtimeFacade,
			org.mockito.Mockito.mock(com.vdt.log_monitoring.modules.alerting.internal.notification.ChatRoomRepository.class),
			org.mockito.Mockito.mock(com.vdt.log_monitoring.modules.alerting.internal.notification.telegram.TelegramNotifier.class));
	}

	private AnomalyDetectedEvent event() {
		return new AnomalyDetectedEvent(
			REPORT_ID,
			APP_ID,
			"checkout-api",
			"Checkout API",
			"ANOMALY_LOG",
			"SECURITY_AUTH_FAILURE",
			"ERROR",
			"Authentication failures exceeded threshold.",
			"Authentication failures exceeded threshold.",
			NOW.minusSeconds(60),
			NOW,
			true,
			"RULE_REQUIRES_AI",
			NOW);
	}

	private AnomalyFacade.AnomalyReportDto report(String aiStatus, Instant updatedAt, String aiError) {
		return new AnomalyFacade.AnomalyReportDto(
			REPORT_ID,
			APP_ID,
			ALERT_ID,
			"ANOMALY_LOG",
			"SECURITY_AUTH_FAILURE",
			"user:john",
			"ERROR",
			"SUCCEEDED".equals(aiStatus) ? "AI_SUCCEEDED" : "AI_FAILED",
			"Authentication failures exceeded threshold.",
			"Authentication failures exceeded threshold.",
			"Possible brute-force attempt.",
			0.85,
			NOW.minusSeconds(60),
			NOW,
			1L,
			NOW.minusSeconds(60),
			NOW,
			"{}",
			true,
			"RULE_REQUIRES_AI",
			aiStatus,
			NOW,
			updatedAt,
			"{}",
			aiError,
			null,
			null,
			NOW,
			updatedAt);
	}
}
