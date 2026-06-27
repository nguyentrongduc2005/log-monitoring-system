package com.vdt.log_monitoring.modules.incident.internal.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.vdt.log_monitoring.modules.incident.api.IncidentException;
import com.vdt.log_monitoring.modules.incident.internal.evidence.IncidentEvidenceCandidate;
import com.vdt.log_monitoring.modules.incident.internal.incident.EvidenceType;
import com.vdt.log_monitoring.modules.incident.internal.incident.IncidentSeverity;

class GeminiIncidentAnalysisClientTest {

	private static final UUID INCIDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");
	private static final UUID APP_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
	private static final Instant WINDOW_START = Instant.parse("2026-06-24T07:30:00Z");
	private static final Instant WINDOW_END = Instant.parse("2026-06-24T08:00:00Z");

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void sendsStructuredJsonSchemaAndLargeOutputBudgetToGemini() {
		RestClient.Builder restClientBuilder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
		server.expect(request -> {
			assertThat(request.getURI().getPath()).isEqualTo("/v1beta/models/gemini-test:generateContent");
			assertThat(request.getURI().getQuery()).contains("key=test-key");
			content().string(allOf(
				containsString("\"maxOutputTokens\":8192"),
				containsString("\"responseMimeType\":\"application/json\""),
				containsString("\"responseSchema\""),
				containsString("\"severity\""),
				containsString("\"suggestedActions\""))).match(request);
		}).andRespond(withSuccess("""
			{
			  "candidates": [
			    {
			      "finishReason": "STOP",
			      "content": {
			        "parts": [
			          {
			            "text": "{\\"summary\\":\\"Payment database timeout\\",\\"likelyCause\\":\\"Postgres connection timeout while saving an order.\\",\\"severity\\":\\"SEV2\\",\\"severityReason\\":\\"Checkout writes are failing.\\",\\"confidence\\":\\"HIGH\\",\\"suggestedActions\\":[\\"Check Postgres connection pool\\"],\\"evidenceRefs\\":[\\"log-1\\"]}"
			          }
			        ]
			      }
			    }
			  ]
			}
			""", MediaType.APPLICATION_JSON));
		GeminiIncidentAnalysisClient client = newClient(restClientBuilder);

		AiIncidentAnalysisResponse response = client.analyze(request());

		assertThat(response.provider()).isEqualTo("gemini");
		assertThat(response.model()).isEqualTo("gemini-test");
		assertThat(response.severity()).isEqualTo(IncidentSeverity.SEV2);
		assertThat(response.suggestedActions()).containsExactly("Check Postgres connection pool");
		server.verify();
	}

	@Test
	void rejectsGeminiResponsesThatWereTruncatedByTokenLimit() {
		RestClient.Builder restClientBuilder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
		server.expect(request -> {
			assertThat(request.getURI().getPath()).isEqualTo("/v1beta/models/gemini-test:generateContent");
		}).andRespond(withSuccess("""
			{
			  "candidates": [
			    {
			      "finishReason": "MAX_TOKENS",
			      "content": {
			        "parts": [
			          {"text": "{\\"summary\\":\\"partial\\",\\"likelyCause\\":\\"cut"}
			        ]
			      }
			    }
			  ]
			}
			""", MediaType.APPLICATION_JSON));
		GeminiIncidentAnalysisClient client = newClient(restClientBuilder);

		assertThatThrownBy(() -> client.analyze(request()))
			.isInstanceOfSatisfying(IncidentException.class, exception -> {
				assertThat(exception.getErrorCode())
					.isEqualTo(IncidentException.ErrorCode.INCIDENT_ANALYSIS_FAILED);
				assertThat(exception.getMessage()).contains("truncated");
			});
		server.verify();
	}

	private GeminiIncidentAnalysisClient newClient(RestClient.Builder restClientBuilder) {
		return new GeminiIncidentAnalysisClient(
			restClientBuilder,
			objectMapper,
			new IncidentAiProperties(
				"gemini",
				"https://gemini.example.test",
				"test-key",
				"gemini-test",
				Duration.ofSeconds(5)));
	}

	private AiIncidentAnalysisRequest request() {
		return new AiIncidentAnalysisRequest(
			INCIDENT_ID,
			"Checkout payment investigation",
			"Manual incident investigation",
			List.of(APP_ID),
			WINDOW_START,
			WINDOW_END,
			List.of(new IncidentEvidenceCandidate(
				EvidenceType.LOG,
				"log-1",
				APP_ID,
				"checkout-timeout",
				"trace-1",
				"ERROR",
				"Payment write failed",
				"ERROR database password=secret token=abc connection timeout",
				WINDOW_END,
				null)));
	}
}
