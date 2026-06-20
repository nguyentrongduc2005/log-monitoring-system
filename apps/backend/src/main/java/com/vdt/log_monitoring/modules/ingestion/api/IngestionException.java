package com.vdt.log_monitoring.modules.ingestion.api;

import org.springframework.http.HttpStatus;

public class IngestionException extends RuntimeException {

    private final ErrorCode errorCode;

    public IngestionException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public enum ErrorCode {
        INVALID_API_KEY(HttpStatus.UNAUTHORIZED),
        API_KEY_NOT_ALLOWED_FOR_APPLICATION(HttpStatus.FORBIDDEN),
        INVALID_RAW_LOG(HttpStatus.BAD_REQUEST),
        INVALID_BATCH(HttpStatus.BAD_REQUEST),
        IDEMPOTENCY_CONFLICT(HttpStatus.CONFLICT),
        INGESTION_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE);

        private final HttpStatus status;

        ErrorCode(HttpStatus status) {
            this.status = status;
        }

        public HttpStatus getStatus() {
            return status;
        }
    }
}
