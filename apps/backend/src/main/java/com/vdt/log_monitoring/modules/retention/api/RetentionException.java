package com.vdt.log_monitoring.modules.retention.api;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class RetentionException extends RuntimeException {

	@Getter
	public enum ErrorCode {
		POLICY_NOT_FOUND(HttpStatus.NOT_FOUND),
		INVALID_POLICY(HttpStatus.BAD_REQUEST),
		RETENTION_DELETE_FAILED(HttpStatus.BAD_GATEWAY);

		private final HttpStatus status;

		ErrorCode(HttpStatus status) {
			this.status = status;
		}
	}

	private final ErrorCode errorCode;

	public RetentionException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public RetentionException(ErrorCode errorCode, String message, Throwable cause) {
		super(message, cause);
		this.errorCode = errorCode;
	}
}
