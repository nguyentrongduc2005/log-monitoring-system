package com.vdt.log_monitoring.api.retention;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.vdt.log_monitoring.modules.retention.api.RetentionFacade;
import com.vdt.log_monitoring.shared.security.JwtTokenProvider;

@WebMvcTest(RetentionController.class)
@Import(RetentionExceptionHandler.class)
class RetentionControllerTest {

	private static final UUID POLICY_ID = UUID.fromString("00000000-0000-0000-0000-000000000501");
	private static final UUID RUN_ID = UUID.fromString("00000000-0000-0000-0000-000000000601");
	private static final Instant NOW = Instant.parse("2026-07-01T00:00:00Z");

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private RetentionFacade retentionFacade;

	@MockBean
	private JwtTokenProvider jwtTokenProvider;

	@Test
	void adminListsRetentionPolicies() throws Exception {
		when(retentionFacade.findPolicies()).thenReturn(List.of(policyDto()));

		mockMvc.perform(get("/api/v1/retention/policies")
				.with(user("admin@example.com").roles("ADMIN")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[0].id").value(POLICY_ID.toString()))
			.andExpect(jsonPath("$.data[0].logLevel").value("INFO"))
			.andExpect(jsonPath("$.data[0].retentionDays").value(7))
			.andExpect(jsonPath("$.data[0].enabled").value(true));
	}

	@Test
	void adminUpdatesFixedRetentionPolicies() throws Exception {
		when(retentionFacade.updatePolicies(any())).thenReturn(List.of(policyDto()));

		mockMvc.perform(put("/api/v1/retention/policies")
				.with(user("admin@example.com").roles("ADMIN"))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					[
					  {
					    "id": "00000000-0000-0000-0000-000000000501",
					    "retentionDays": 14,
					    "enabled": true
					  }
					]
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[0].logLevel").value("INFO"));

		ArgumentCaptor<List<RetentionFacade.UpdateRetentionPolicyCommand>> captor = ArgumentCaptor.captor();
		verify(retentionFacade).updatePolicies(captor.capture());
		org.assertj.core.api.Assertions.assertThat(captor.getValue().getFirst().id()).isEqualTo(POLICY_ID);
		org.assertj.core.api.Assertions.assertThat(captor.getValue().getFirst().retentionDays()).isEqualTo(14);
		org.assertj.core.api.Assertions.assertThat(captor.getValue().getFirst().enabled()).isTrue();
	}

	@Test
	void adminRunsPolicyImmediately() throws Exception {
		when(retentionFacade.runPolicy(POLICY_ID)).thenReturn(runDto());

		mockMvc.perform(post("/api/v1/retention/policies/{id}/run", POLICY_ID)
				.with(user("admin@example.com").roles("ADMIN"))
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.policyId").value(POLICY_ID.toString()))
			.andExpect(jsonPath("$.data.affectedRows").value(12));
	}

	private static RetentionFacade.RetentionPolicyDto policyDto() {
		return new RetentionFacade.RetentionPolicyDto(
			POLICY_ID,
			"INFO",
			"INFO logs",
			"Routine application logs and request traces.",
			7,
			1,
			365,
			true,
			NOW.plusSeconds(3600),
			runDto()
		);
	}

	private static RetentionFacade.RetentionRunDto runDto() {
		return new RetentionFacade.RetentionRunDto(
			RUN_ID,
			POLICY_ID,
			"SUCCESS",
			NOW,
			NOW.plusSeconds(1),
			12,
			"Deleted 12 expired INFO logs."
		);
	}
}
