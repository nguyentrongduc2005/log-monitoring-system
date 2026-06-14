package com.vdt.log_monitoring.api.logs;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.vdt.log_monitoring.modules.logs.api.LogsException;
import com.vdt.log_monitoring.shared.dto.ApiResponse;

@RestControllerAdvice(basePackages = "com.vdt.log_monitoring.api.logs")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LogsExceptionHandler {

    @ExceptionHandler(LogsException.class)
    public ResponseEntity<ApiResponse<Object>> handleLogsException(LogsException ex) {
        HttpStatus status = switch (ex.getErrorCode()) {
            case INVALID_API_KEY -> HttpStatus.UNAUTHORIZED;
            case API_KEY_NOT_ALLOWED_FOR_APPLICATION -> HttpStatus.FORBIDDEN;
            case INVALID_RAW_LOG, INVALID_BATCH -> HttpStatus.BAD_REQUEST;
            case IDEMPOTENCY_CONFLICT -> HttpStatus.CONFLICT;
            case INGESTION_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
        };

        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .data(ex.getErrorCode().name())
                .build();

        return new ResponseEntity<>(response, status);
    }
}
