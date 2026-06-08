package com.vdt.log_monitoring.modules.identity.application;

import lombok.Getter;

@Getter
public class IdentityException extends RuntimeException {

	public enum ErrorCode {
		USER_NOT_FOUND,
		EMAIL_ALREADY_EXISTS,
		INVALID_CREDENTIALS,
		ACCOUNT_DISABLED,
		ACCOUNT_LOCKED,
		UNAUTHORIZED
	}

	private final ErrorCode errorCode;

	public IdentityException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}
}
