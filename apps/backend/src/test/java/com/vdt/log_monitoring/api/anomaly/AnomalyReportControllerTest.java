package com.vdt.log_monitoring.api.anomaly;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.vdt.log_monitoring.modules.alerting.api.AlertingFacade;
import com.vdt.log_monitoring.modules.alerting.api.AlertingException;
import com.vdt.log_monitoring.modules.anomaly.api.AnomalyFacade;
import com.vdt.log_monitoring.modules.identity.api.ApplicationAccessFacade;
import com.vdt.log_monitoring.modules.identity.api.IdentityFacade;
import com.vdt.log_monitoring.shared.security.JwtTokenProvider;

@WebMvcTest(AnomalyReportController.class)
@Import(AnomalyExceptionHandler.class)
class AnomalyReportControllerTest {

	private static final Instant NOW = Instant.parse("2026-06-29T00:00:00Z");
	private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID APP_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
	private static final UUID REPORT_ID = UUID.fromString("00000000-0000-0000-0000-000000000301");
	private static final UUID ALERT_ID = UUID.fromString("00000000-0000-0000-0000-000000000401");

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private AnomalyFacade anomalyFacade;

	@MockBean
	private AlertingFacade alertingFacade;

	@MockBean
	private IdentityFacade identityFacade;

	@MockBean
	private ApplicationAccessFacade applicationAccessFacade;

	@MockBean
	private JwtTokenProvider jwtTokenProvider;

	@Test
	void resolvesReportAndLinkedAlert() throws Exception {
		when(identityFacade.findUserByEmail("engineer@example.com")).thenReturn(userDto());
		when(applicationAccessFacade.findVisibleApplications(USER_ID, "ENGINEER"))
			.thenReturn(List.of(applicationDto()));
		when(anomalyFacade.findReportById(REPORT_ID)).thenReturn(reportDto("ALERTED", ALERT_ID));
		when(anomalyFacade.resolveReport(REPORT_ID, USER_ID)).thenReturn(reportDto("RESOLVED", ALERT_ID));

		mockMvc.perform(put("/api/v1/anomaly/reports/{id}/resolve", REPORT_ID)
				.with(user("engineer@example.com").roles("ENGINEER"))
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.status").value("RESOLVED"))
			.andExpect(jsonPath("$.data.resolvedBy").value(USER_ID.toString()));

		verify(alertingFacade).resolveAlert(ALERT_ID, USER_ID);
	}

	@Test
	void resolvesReportWithoutAlertWhenReportIsNotLinked() throws Exception {
		when(identityFacade.findUserByEmail("engineer@example.com")).thenReturn(userDto());
		when(applicationAccessFacade.findVisibleApplications(USER_ID, "ENGINEER"))
			.thenReturn(List.of(applicationDto()));
		when(anomalyFacade.findReportById(REPORT_ID)).thenReturn(reportDto("DETECTED", null));
		when(anomalyFacade.resolveReport(REPORT_ID, USER_ID)).thenReturn(reportDto("RESOLVED", null));

		mockMvc.perform(put("/api/v1/anomaly/reports/{id}/resolve", REPORT_ID)
				.with(user("engineer@example.com").roles("ENGINEER"))
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.status").value("RESOLVED"));

		verify(alertingFacade, never()).resolveAlert(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
	}

	@Test
	void resolvesReportWhenLinkedAlertNoLongerExists() throws Exception {
		when(identityFacade.findUserByEmail("engineer@example.com")).thenReturn(userDto());
		when(applicationAccessFacade.findVisibleApplications(USER_ID, "ENGINEER"))
			.thenReturn(List.of(applicationDto()));
		when(anomalyFacade.findReportById(REPORT_ID)).thenReturn(reportDto("ALERTED", ALERT_ID));
		when(anomalyFacade.resolveReport(REPORT_ID, USER_ID)).thenReturn(reportDto("RESOLVED", ALERT_ID));
		doThrow(new AlertingException(AlertingException.ErrorCode.ALERT_NOT_FOUND, "Alert not found"))
			.when(alertingFacade).resolveAlert(ALERT_ID, USER_ID);

		mockMvc.perform(put("/api/v1/anomaly/reports/{id}/resolve", REPORT_ID)
				.with(user("engineer@example.com").roles("ENGINEER"))
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.status").value("RESOLVED"))
			.andExpect(jsonPath("$.data.resolvedBy").value(USER_ID.toString()));

		verify(alertingFacade).resolveAlert(ALERT_ID, USER_ID);
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
			NOW);
	}

	private static ApplicationAccessFacade.ApplicationDto applicationDto() {
		return new ApplicationAccessFacade.ApplicationDto(
			APP_ID, "checkout-api", "Checkout API", null, "ACTIVE", NOW, NOW);
	}

	private static AnomalyFacade.AnomalyReportDto reportDto(String status, UUID alertId) {
		return new AnomalyFacade.AnomalyReportDto(
			REPORT_ID,
			APP_ID,
			alertId,
			"ANOMALY_LOG",
			"SECURITY_AUTH_FAILURE",
			"user:john",
			"ERROR",
			status,
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
			"NOT_REQUESTED",
			null,
			null,
			null,
			null,
			"RESOLVED".equals(status) ? USER_ID : null,
			"RESOLVED".equals(status) ? NOW : null,
			NOW,
			NOW);
	}
}
