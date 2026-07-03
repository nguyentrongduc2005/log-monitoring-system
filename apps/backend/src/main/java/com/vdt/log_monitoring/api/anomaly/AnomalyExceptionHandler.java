package com.vdt.log_monitoring.api.anomaly;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.vdt.log_monitoring.modules.anomaly.api.AnomalyException;
import com.vdt.log_monitoring.shared.dto.ApiResponse;

@RestControllerAdvice(basePackages = "com.vdt.log_monitoring.api.anomaly")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AnomalyExceptionHandler {

    @ExceptionHandler(AnomalyException.class)
    public ResponseEntity<ApiResponse<Object>> handleAnomalyException(AnomalyException ex) {
        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .data(ex.getErrorCode().name())
                .build();

        return new ResponseEntity<>(response, ex.getErrorCode().getStatus());
    }
}
