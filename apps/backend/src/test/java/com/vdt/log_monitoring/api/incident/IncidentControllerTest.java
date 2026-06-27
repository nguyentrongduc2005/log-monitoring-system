package com.vdt.log_monitoring.api.incident;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.vdt.log_monitoring.modules.alerting.api.AlertingFacade;
import com.vdt.log_monitoring.modules.identity.api.ApplicationAccessFacade;
import com.vdt.log_monitoring.modules.identity.api.IdentityFacade;
import com.vdt.log_monitoring.modules.incident.api.IncidentFacade;
import com.vdt.log_monitoring.shared.security.JwtTokenProvider;

@WebMvcTest(IncidentController.class)
@Import(IncidentExceptionHandler.class)
class IncidentControllerTest {

	private static final Instant NOW = Instant.parse("2026-06-24T08:00:00Z");
	private static final Instant WINDOW_START = Instant.parse("2026-06-24T07:30:00Z");
	private static final Instant WINDOW_END = Instant.parse("2026-06-24T08:00:00Z");
	private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID APP_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
	private static final UUID INCIDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");
	private static final UUID ALERT_ID = UUID.fromString("00000000-0000-0000-0000-000000000301");

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private IncidentFacade incidentFacade;

	@MockBean
	private AlertingFacade alertingFacade;

	@MockBean
	private IdentityFacade identityFacade;

	@MockBean
	private ApplicationAccessFacade applicationAccessFacade;

	@MockBean
	private JwtTokenProvider jwtTokenProvider;

	@Test
	void authenticatedUserStartsInvestigationFromVisibleAlert() throws Exception {
		when(identityFacade.findUserByEmail("engineer@example.com")).thenReturn(userDto());
		when(applicationAccessFacade.findVisibleApplications(USER_ID, "ENGINEER"))
			.thenReturn(List.of(applicationDto()));
		when(alertingFacade.findAlertById(ALERT_ID)).thenReturn(alertDto());
		when(incidentFacade.startFromAlert(org.mockito.ArgumentMatchers.any()))
			.thenReturn(incidentDto("INVESTIGATING"));

		mockMvc.perform(post("/api/v1/incidents/from-alert/{alertId}", ALERT_ID)
				.with(user("engineer@example.com").roles("ENGINEER"))
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.id").value(INCIDENT_ID.toString()))
			.andExpect(jsonPath("$.data.status").value("INVESTIGATING"))
			.andExpect(jsonPath("$.data.shortSummary").value("Payment checkout is failing."))
			.andExpect(jsonPath("$.data.impact").value("Checkout requests are affected."))
			.andExpect(jsonPath("$.data.recommendedActions[0]").value("Check payment provider health."))
			.andExpect(jsonPath("$.data.errorLogs[0].fingerprint").value("checkout-payment"));

		ArgumentCaptor<IncidentFacade.StartFromAlertCommand> captor =
			ArgumentCaptor.forClass(IncidentFacade.StartFromAlertCommand.class);
		verify(incidentFacade).startFromAlert(captor.capture());
		org.assertj.core.api.Assertions.assertThat(captor.getValue().alertId()).isEqualTo(ALERT_ID);
		org.assertj.core.api.Assertions.assertThat(captor.getValue().applicationId()).isEqualTo(APP_ID);
	}

	@Test
	void authenticatedUserListsVisibleIncidents() throws Exception {
		when(identityFacade.findUserByEmail("engineer@example.com")).thenReturn(userDto());
		when(applicationAccessFacade.findVisibleApplications(USER_ID, "ENGINEER"))
			.thenReturn(List.of(applicationDto()));
		when(incidentFacade.findIncidents(List.of(APP_ID), "INVESTIGATING", "SEV2"))
			.thenReturn(List.of(incidentSummaryDto()));

		mockMvc.perform(get("/api/v1/incidents")
				.with(user("engineer@example.com").roles("ENGINEER"))
				.param("status", "INVESTIGATING")
				.param("severity", "SEV2"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[0].id").value(INCIDENT_ID.toString()))
			.andExpect(jsonPath("$.data[0].shortSummary").value("Payment checkout is failing."))
			.andExpect(jsonPath("$.data[0].impact").value("Checkout requests are affected."));
	}

	@Test
	void authenticatedUserResolvesVisibleIncident() throws Exception {
		when(identityFacade.findUserByEmail("engineer@example.com")).thenReturn(userDto());
		when(applicationAccessFacade.findVisibleApplications(USER_ID, "ENGINEER"))
			.thenReturn(List.of(applicationDto()));
		when(incidentFacade.findIncidentById(INCIDENT_ID)).thenReturn(incidentDto("INVESTIGATING"));
		when(incidentFacade.resolveIncident(INCIDENT_ID, USER_ID)).thenReturn(incidentDto("RESOLVED"));

		mockMvc.perform(put("/api/v1/incidents/{id}/resolve", INCIDENT_ID)
				.with(user("engineer@example.com").roles("ENGINEER"))
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.status").value("RESOLVED"));
	}

	private static IdentityFacade.UserDto userDto() {
		return new IdentityFacade.UserDto(
			USER_ID,
			"engineer@example.com",
			"Engineer",
			"ENGINEER",
			"ACTIVE",
			NOW,
			NOW,
			NOW
		);
	}

	private static ApplicationAccessFacade.ApplicationDto applicationDto() {
		return new ApplicationAccessFacade.ApplicationDto(
			APP_ID, "checkout-api", "Checkout API", null, "ACTIVE", NOW, NOW);
	}

	private static AlertingFacade.AlertDto alertDto() {
		return new AlertingFacade.AlertDto(
			ALERT_ID,
			UUID.randomUUID(),
			"Checkout API error rate high",
			APP_ID,
			"checkout-api",
			"Checkout API",
			"ERROR",
			List.of(new com.vdt.log_monitoring.api.alerting.dto.AlertLogSampleDto("ERROR", "Payment failed")),
			WINDOW_END,
			3,
			WINDOW_START,
			WINDOW_END,
			"OPEN",
			List.of("WEBSOCKET"),
			List.of(),
			null, null, null, null,
			WINDOW_START,
			WINDOW_END
		);
	}

	private static IncidentFacade.IncidentSummaryDto incidentSummaryDto() {
		return new IncidentFacade.IncidentSummaryDto(
			INCIDENT_ID,
			"Checkout payment investigation",
			null,
			"Payment checkout is failing.",
			"Checkout requests are affected.",
			"INVESTIGATING",
			"SEV2",
			"APPLICATION",
			"MANUAL",
			NOW,
			WINDOW_START,
			WINDOW_END,
			WINDOW_END,
			List.of(APP_ID),
			USER_ID,
			null,
			null,
			NOW,
			NOW);
	}

	private static IncidentFacade.IncidentDto incidentDto(String status) {
		return new IncidentFacade.IncidentDto(
			INCIDENT_ID,
			"Checkout payment investigation",
			null,
			"Payment checkout is failing.",
			"Checkout requests are affected.",
			"Payment provider timeout",
			List.of("Check payment provider health."),
			status,
			"SEV2",
			"APPLICATION",
			"MANUAL",
			NOW,
			WINDOW_START,
			WINDOW_END,
			WINDOW_END,
			List.of(new IncidentFacade.ApplicationImpactDto(APP_ID, "PRIMARY", NOW)),
			List.of(new IncidentFacade.ErrorLogDto(
				UUID.randomUUID(),
				APP_ID,
				"checkout-api",
				"Checkout API",
				"ERROR",
				"raw checkout error",
				"checkout-payment",
				"trace-1",
				WINDOW_END)),
			List.of(),
			USER_ID,
			status.equals("RESOLVED") ? USER_ID : null,
			status.equals("RESOLVED") ? NOW : null,
			NOW,
			NOW);
	}
}
