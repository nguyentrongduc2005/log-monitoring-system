package com.vdt.log_monitoring.modules.anomaly.internal.metric;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.OptionalDouble;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class PrometheusMetricServiceTest {

	private static final UUID APP_ID = UUID.fromString("00000000-0000-0000-0000-000000000103");

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void returnsFirstNumericValueFromPrometheusVector() {
		RestClient.Builder restClientBuilder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
		server.expect(request -> {
			assertThat(request.getURI().getPath()).isEqualTo("/api/v1/query");
			assertThat(request.getURI().getRawQuery()).contains("query=");
			assertThat(request.getURI().getRawQuery()).contains("application_id");
			assertThat(request.getURI().getRawQuery()).contains(APP_ID.toString());
			assertThat(request.getURI().getRawQuery()).doesNotContain(" ");
		}).andRespond(withSuccess("""
			{
			  "status": "success",
			  "data": {
			    "result": [
			      {"value": [1719532800, "91.2"]}
			    ]
			  }
			}
			""", MediaType.APPLICATION_JSON));
		PrometheusMetricService service = new PrometheusMetricService(
			restClientBuilder,
			objectMapper,
			"http://prometheus.test");

		OptionalDouble result = service.getCpuUsage(APP_ID);

		assertThat(result).hasValue(91.2);
		server.verify();
	}

	@Test
	void returnsEmptyForEmptyVector() {
		RestClient.Builder restClientBuilder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
		server.expect(request -> assertThat(request.getURI().getPath()).isEqualTo("/api/v1/query"))
			.andRespond(withSuccess("""
				{"status":"success","data":{"result":[]}}
				""", MediaType.APPLICATION_JSON));
		PrometheusMetricService service = new PrometheusMetricService(
			restClientBuilder,
			objectMapper,
			"http://prometheus.test");

		OptionalDouble result = service.getCpuUsage(APP_ID);

		assertThat(result).isEmpty();
		server.verify();
	}

	@Test
	void returnsEmptyForHttpFailure() {
		RestClient.Builder restClientBuilder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
		server.expect(request -> assertThat(request.getURI().getPath()).isEqualTo("/api/v1/query"))
			.andRespond(withServerError());
		PrometheusMetricService service = new PrometheusMetricService(
			restClientBuilder,
			objectMapper,
			"http://prometheus.test");

		OptionalDouble result = service.getCpuUsage(APP_ID);

		assertThat(result).isEmpty();
		server.verify();
	}
}
