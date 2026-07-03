package com.vdt.log_monitoring.modules.anomaly.api;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class AnomalyException extends RuntimeException {

    @Getter
    public enum ErrorCode {
        ANOMALY_RULE_NOT_FOUND(HttpStatus.NOT_FOUND),
        ANOMALY_REPORT_NOT_FOUND(HttpStatus.NOT_FOUND),
        INVALID_ANOMALY_CONFIGURATION(HttpStatus.BAD_REQUEST),
        ANOMALY_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR),
        METRIC_COLLECTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR);

        private final HttpStatus status;

        ErrorCode(HttpStatus status) {
            this.status = status;
        }
    }

    private final ErrorCode errorCode;

    public AnomalyException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public AnomalyException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
