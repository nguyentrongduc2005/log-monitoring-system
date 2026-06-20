package com.vdt.log_monitoring.modules.processing.api;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
public class ProcessingException extends RuntimeException {

    private final ErrorCode errorCode;

    public ProcessingException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ProcessingException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    @Getter
    @RequiredArgsConstructor
    public enum ErrorCode {
        UNSUPPORTED_RAW_EVENT_SCHEMA(HttpStatus.BAD_REQUEST),
        RAW_LOG_PARSE_FAILED(HttpStatus.BAD_REQUEST),
        RAW_LOG_NORMALIZATION_FAILED(HttpStatus.BAD_REQUEST),
        LOG_STORAGE_FAILED(HttpStatus.SERVICE_UNAVAILABLE),
        DOWNSTREAM_EVENT_PUBLISH_FAILED(HttpStatus.SERVICE_UNAVAILABLE),
        PROCESSING_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE);

        private final HttpStatus status;
    }
}
