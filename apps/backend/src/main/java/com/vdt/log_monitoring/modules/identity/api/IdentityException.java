package com.vdt.log_monitoring.modules.identity.api;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class IdentityException extends RuntimeException {

	@Getter
	public enum ErrorCode {
		USER_NOT_FOUND(HttpStatus.NOT_FOUND),
		EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT),
		INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
		ACCOUNT_DISABLED(HttpStatus.FORBIDDEN),
		ACCOUNT_LOCKED(HttpStatus.FORBIDDEN),
		UNAUTHORIZED(HttpStatus.FORBIDDEN),
		APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND),
		APPLICATION_NAME_ALREADY_EXISTS(HttpStatus.CONFLICT),
		APPLICATION_INACTIVE(HttpStatus.FORBIDDEN),
		APPLICATION_ACCESS_NOT_FOUND(HttpStatus.NOT_FOUND),
		INVALID_APPLICATION_STATUS(HttpStatus.BAD_REQUEST),
		INVALID_APPLICATION_ACCESS_LEVEL(HttpStatus.BAD_REQUEST),
		INVALID_APPLICATION_ACCESS_GRANT(HttpStatus.BAD_REQUEST),
		API_KEY_NOT_FOUND(HttpStatus.NOT_FOUND),
		INVALID_API_KEY(HttpStatus.UNAUTHORIZED),
		API_KEY_REVOKED(HttpStatus.FORBIDDEN),
		API_KEY_EXPIRED(HttpStatus.FORBIDDEN);

		private final HttpStatus status;

		ErrorCode(HttpStatus status) {
			this.status = status;
		}
	}

	private final ErrorCode errorCode;

	public IdentityException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}
}
