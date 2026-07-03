package com.vdt.log_monitoring.api.alerting;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.vdt.log_monitoring.modules.alerting.api.AlertingException;
import com.vdt.log_monitoring.shared.dto.ApiResponse;

class AlertingExceptionHandlerTest {

	private final AlertingExceptionHandler handler = new AlertingExceptionHandler();

	@Test
	void mapsAlertingExceptionToApiResponse() {
		ResponseEntity<ApiResponse<Object>> response = handler.handleAlertingException(
			new AlertingException(
				AlertingException.ErrorCode.CHAT_ROOM_NOT_FOUND,
				"Chat room not found"
			)
		);

		assertThat(response.getStatusCode()).isEqualTo(AlertingException.ErrorCode.CHAT_ROOM_NOT_FOUND.getStatus());
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().isSuccess()).isFalse();
		assertThat(response.getBody().getMessage()).isEqualTo("Chat room not found");
		assertThat(response.getBody().getData()).isEqualTo("CHAT_ROOM_NOT_FOUND");
	}
}
