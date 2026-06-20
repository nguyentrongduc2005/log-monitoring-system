package com.vdt.log_monitoring.api.ingestion;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.vdt.log_monitoring.modules.ingestion.api.IngestionException;
import com.vdt.log_monitoring.shared.dto.ApiResponse;

@RestControllerAdvice(basePackages = "com.vdt.log_monitoring.api.ingestion")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IngestionExceptionHandler {

    @ExceptionHandler(IngestionException.class)
    public ResponseEntity<ApiResponse<Object>> handleIngestionException(IngestionException ex) {
        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .data(ex.getErrorCode().name())
                .build();

        return new ResponseEntity<>(response, ex.getErrorCode().getStatus());
    }
}
