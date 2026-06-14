package com.vdt.log_monitoring.modules.identity.api;

import lombok.Getter;

@Getter
public class IdentityException extends RuntimeException {

	public enum ErrorCode {
		USER_NOT_FOUND,
		EMAIL_ALREADY_EXISTS,
		INVALID_CREDENTIALS,
		ACCOUNT_DISABLED,
		ACCOUNT_LOCKED,
		UNAUTHORIZED,
		APPLICATION_NOT_FOUND,
		APPLICATION_NAME_ALREADY_EXISTS,
		APPLICATION_INACTIVE,
		APPLICATION_ACCESS_NOT_FOUND,
		INVALID_APPLICATION_STATUS,
		INVALID_APPLICATION_ACCESS_LEVEL,
		INVALID_APPLICATION_ACCESS_GRANT,
		API_KEY_NOT_FOUND,
		INVALID_API_KEY,
		API_KEY_REVOKED,
		API_KEY_EXPIRED
	}

	private final ErrorCode errorCode;

	public IdentityException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}
}
