package com.vdt.log_monitoring.api.identity;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.vdt.log_monitoring.modules.identity.api.IdentityException;
import com.vdt.log_monitoring.shared.dto.ApiResponse;

@RestControllerAdvice(basePackages = "com.vdt.log_monitoring.api.identity")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IdentityExceptionHandler {

	@ExceptionHandler(IdentityException.class)
	public ResponseEntity<ApiResponse<Object>> handleIdentityException(IdentityException ex) {
		ApiResponse<Object> response = ApiResponse.builder()
				.success(false)
				.message(ex.getMessage())
				.data(ex.getErrorCode().name())
				.build();

		return new ResponseEntity<>(response, ex.getErrorCode().getStatus());
	}
}
