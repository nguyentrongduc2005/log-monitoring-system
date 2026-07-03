package com.vdt.log_monitoring.modules.incident.internal.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vdt.log_monitoring.modules.incident.api.IncidentFacade;
import com.vdt.log_monitoring.modules.incident.internal.evidence.IncidentEvidenceCollector;
import com.vdt.log_monitoring.modules.incident.internal.investigation.IncidentAnalysisRunner;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

	private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID APP_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
	private static final UUID ALERT_2_ID = UUID.fromString("00000000-0000-0000-0000-000000000302");
	private static final Instant NOW = Instant.parse("2026-06-24T08:00:00Z");

	@Mock
	private IncidentRepository incidentRepository;

	@Mock
	private IncidentEvidenceCollector evidenceCollector;

	@Mock
	private IncidentAnalysisRunner analysisRunner;



	@Test
	void startFromAlertCreatesNewIncident() {
		when(incidentRepository.findOpenByAlertId(ALERT_2_ID)).thenReturn(List.of());
		when(incidentRepository.save(org.mockito.ArgumentMatchers.any(IncidentEntity.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));
		when(evidenceCollector.collectFromTriggerAlert(
			org.mockito.ArgumentMatchers.eq(ALERT_2_ID),
			org.mockito.ArgumentMatchers.eq(APP_ID),
			org.mockito.ArgumentMatchers.any(Instant.class),
			org.mockito.ArgumentMatchers.any(Instant.class)))
			.thenReturn(List.of());

		IncidentService service = new IncidentService(incidentRepository, evidenceCollector, analysisRunner);
		IncidentEntity result = service.startFromAlert(command(ALERT_2_ID));

		assertThat(result).isNotNull();
		assertThat(result.applicationIds()).containsExactly(APP_ID);
		verify(incidentRepository).save(result);
	}

	private IncidentFacade.StartFromAlertCommand command(UUID alertId) {
		return new IncidentFacade.StartFromAlertCommand(
			alertId,
			APP_ID,
			"payment-service",
			"Payment Service",
			"ERROR",
			List.of(new com.vdt.log_monitoring.api.alerting.dto.AlertLogSampleDto("ERROR", "Payment failed")),
			NOW,
			USER_ID);
	}
}
