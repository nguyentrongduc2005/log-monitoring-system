package com.vdt.log_monitoring.api.identity;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.vdt.log_monitoring.modules.identity.application.IdentityException;
import com.vdt.log_monitoring.shared.dto.ApiResponse;

@RestControllerAdvice(basePackages = "com.vdt.log_monitoring.api.identity")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IdentityExceptionHandler {

	@ExceptionHandler(IdentityException.class)
	public ResponseEntity<ApiResponse<Object>> handleIdentityException(IdentityException ex) {
		HttpStatus status = switch (ex.getErrorCode()) {
			case USER_NOT_FOUND -> HttpStatus.NOT_FOUND;
			case EMAIL_ALREADY_EXISTS -> HttpStatus.BAD_REQUEST;
			case INVALID_CREDENTIALS -> HttpStatus.UNAUTHORIZED;
			case ACCOUNT_DISABLED, ACCOUNT_LOCKED, UNAUTHORIZED -> HttpStatus.FORBIDDEN;
		};

		ApiResponse<Object> response = ApiResponse.builder()
				.success(false)
				.message(ex.getMessage())
				.data(ex.getErrorCode().name())
				.build();

		return new ResponseEntity<>(response, status);
	}
}
