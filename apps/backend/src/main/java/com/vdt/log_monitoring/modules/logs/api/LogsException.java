package com.vdt.log_monitoring.modules.logs.api;

public class LogsException extends RuntimeException {

    private final ErrorCode errorCode;

    public LogsException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public enum ErrorCode {
        INVALID_API_KEY,
        API_KEY_NOT_ALLOWED_FOR_APPLICATION,
        INVALID_RAW_LOG,
        INVALID_BATCH,
        IDEMPOTENCY_CONFLICT,
        INGESTION_UNAVAILABLE
    }
}
