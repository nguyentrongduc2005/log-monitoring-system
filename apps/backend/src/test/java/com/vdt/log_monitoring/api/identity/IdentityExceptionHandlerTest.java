package com.vdt.log_monitoring.api.identity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.vdt.log_monitoring.modules.identity.api.IdentityException;

class IdentityExceptionHandlerTest {

	private final IdentityExceptionHandler handler = new IdentityExceptionHandler();

	@Test
	void usesStatusDeclaredByErrorCode() {
		IdentityException exception = new IdentityException(
			IdentityException.ErrorCode.EMAIL_ALREADY_EXISTS,
			"Email is already registered"
		);

		var response = handler.handleIdentityException(exception);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(response.getBody().getData()).isEqualTo("EMAIL_ALREADY_EXISTS");
	}
}
