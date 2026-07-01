package com.vdt.log_monitoring.api.retention;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.vdt.log_monitoring.modules.retention.api.RetentionException;
import com.vdt.log_monitoring.shared.dto.ApiResponse;

@RestControllerAdvice(basePackages = "com.vdt.log_monitoring.api.retention")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RetentionExceptionHandler {

	@ExceptionHandler(RetentionException.class)
	public ResponseEntity<ApiResponse<Object>> handleRetentionException(RetentionException exception) {
		ApiResponse<Object> response = ApiResponse.builder()
			.success(false)
			.message(exception.getMessage())
			.data(exception.getErrorCode().name())
			.build();

		return new ResponseEntity<>(response, exception.getErrorCode().getStatus());
	}
}
