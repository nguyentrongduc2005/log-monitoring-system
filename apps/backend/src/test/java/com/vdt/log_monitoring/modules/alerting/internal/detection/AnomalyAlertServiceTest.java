package com.vdt.log_monitoring.modules.alerting.internal.detection;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdt.log_monitoring.modules.alerting.internal.alert.AlertEntity;
import com.vdt.log_monitoring.modules.alerting.internal.alert.AlertLogSample;
import com.vdt.log_monitoring.modules.alerting.internal.alert.AlertService;
import com.vdt.log_monitoring.modules.alerting.internal.notification.websocket.WebSocketAlertPublisher;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertChannel;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertDeliveryTarget;
import com.vdt.log_monitoring.modules.alerting.internal.rule.AlertSeverity;
import com.vdt.log_monitoring.modules.anomaly.api.AnomalyFacade;
import com.vdt.log_monitoring.modules.anomaly.api.events.AnomalyDetectedEvent;

@ExtendWith(MockitoExtension.class)
class AnomalyAlertServiceTest {

	private static final UUID REPORT_ID = UUID.fromString("00000000-0000-0000-0000-000000000301");
	private static final UUID APP_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
	private static final Instant DETECTED_AT = Instant.parse("2026-06-29T00:00:00Z");

	@Mock
	private AlertService alertService;
	@Mock
	private WebSocketAlertPublisher webSocketAlertPublisher;
	@Mock
	private AnomalyFacade anomalyFacade;
	@Mock
	private AnomalyAiWorkflowService anomalyAiWorkflowService;

	@Test
	void startsAiWorkflowWhenAnomalyRequestsAi() {
		AnomalyAlertService service = service();
		AnomalyDetectedEvent event = event(true);
		AlertEntity alert = alert();
		when(anomalyFacade.findReportById(REPORT_ID)).thenReturn(reportWithSamples());
		when(alertService.createFromAnomaly(eq(event), any(), any())).thenReturn(alert);

		service.createOrUpdateAlert(event);

		verify(webSocketAlertPublisher).publish(alert);
		verify(anomalyFacade).markAlerted(REPORT_ID, alert.getId());
		verify(anomalyAiWorkflowService).requestAnalysis(event, alert.getId());

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Set<AlertDeliveryTarget>> targets = ArgumentCaptor.forClass(Set.class);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<AlertLogSample>> samples = ArgumentCaptor.forClass(List.class);
		verify(alertService).createFromAnomaly(eq(event), targets.capture(), samples.capture());
		org.assertj.core.api.Assertions.assertThat(targets.getValue())
			.containsExactly(AlertDeliveryTarget.channelOnly(AlertChannel.WEBSOCKET));
		org.assertj.core.api.Assertions.assertThat(samples.getValue())
			.containsExactly(
				new AlertLogSample("WARN", "Authentication failed for user john"),
				new AlertLogSample("INFO", "Authentication failed for user jane"));
	}

	@Test
	void skipsAiWorkflowWhenAnomalyDoesNotRequestAi() {
		AnomalyAlertService service = service();
		AnomalyDetectedEvent event = event(false);
		AlertEntity alert = alert();
		when(anomalyFacade.findReportById(REPORT_ID)).thenReturn(reportWithLegacySampleMessages());
		when(alertService.createFromAnomaly(eq(event), any(), any())).thenReturn(alert);

		service.createOrUpdateAlert(event);

		verify(anomalyFacade).markAlerted(REPORT_ID, alert.getId());
		verify(anomalyAiWorkflowService, never()).requestAnalysis(any(), any());

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<AlertLogSample>> samples = ArgumentCaptor.forClass(List.class);
		verify(alertService).createFromAnomaly(eq(event), any(), samples.capture());
		org.assertj.core.api.Assertions.assertThat(samples.getValue())
			.containsExactly(new AlertLogSample("UNKNOWN", "Legacy authentication sample"));
	}

	private AnomalyAlertService service() {
		return new AnomalyAlertService(
			alertService,
			webSocketAlertPublisher,
			anomalyFacade,
			anomalyAiWorkflowService,
			new ObjectMapper(),
			org.mockito.Mockito.mock(com.vdt.log_monitoring.modules.alerting.internal.notification.ChatRoomRepository.class),
			org.mockito.Mockito.mock(com.vdt.log_monitoring.modules.alerting.internal.notification.telegram.TelegramNotifier.class));
	}

	private AlertEntity alert() {
		return AlertEntity.createFromAnomaly(
			APP_ID,
			"checkout-api",
			"Checkout API",
			"ANOMALY_LOG",
			REPORT_ID,
			"SECURITY_AUTH_FAILURE",
			AlertSeverity.ERROR,
			"Authentication failures exceeded threshold.",
			"{}",
			List.of(),
			DETECTED_AT,
			DETECTED_AT,
			DETECTED_AT,
			Set.of(AlertDeliveryTarget.channelOnly(AlertChannel.WEBSOCKET)));
	}

	private AnomalyFacade.AnomalyReportDto reportWithSamples() {
		return report("{\"logSamples\":["
			+ "{\"level\":\"WARN\",\"message\":\"Authentication failed for user john\"},"
			+ "{\"level\":\"INFO\",\"message\":\"Authentication failed for user jane\"}"
			+ "]}");
	}

	private AnomalyFacade.AnomalyReportDto reportWithLegacySampleMessages() {
		return report("{\"sampleMessages\":[\"Legacy authentication sample\"]}");
	}

	private AnomalyFacade.AnomalyReportDto report(String evidencePayloadJson) {
		return new AnomalyFacade.AnomalyReportDto(
			REPORT_ID,
			APP_ID,
			null,
			"ANOMALY_LOG",
			"SECURITY_AUTH_FAILURE",
			"user:john",
			"ERROR",
			"DETECTED",
			"Authentication failures exceeded threshold.",
			"Authentication failures exceeded threshold.",
			null,
			0.9,
			DETECTED_AT.minusSeconds(60),
			DETECTED_AT,
			1,
			DETECTED_AT.minusSeconds(60),
			DETECTED_AT,
			evidencePayloadJson,
			false,
			null,
			"NOT_REQUESTED",
			null,
			null,
			null,
			null,
			null,
			null,
			DETECTED_AT,
			DETECTED_AT);
	}

	private AnomalyDetectedEvent event(boolean aiTriggerRequested) {
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
			DETECTED_AT.minusSeconds(60),
			DETECTED_AT,
			aiTriggerRequested,
			aiTriggerRequested ? "RULE_REQUIRES_AI" : null,
			DETECTED_AT);
	}
}
