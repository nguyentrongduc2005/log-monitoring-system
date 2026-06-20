package com.vdt.log_monitoring.modules.alerting.api;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class AlertingException extends RuntimeException {

	@Getter
	public enum ErrorCode {
		ALERT_RULE_NOT_FOUND(HttpStatus.NOT_FOUND),
		ALERT_NOT_FOUND(HttpStatus.NOT_FOUND),
		ALERT_RULE_NAME_ALREADY_EXISTS(HttpStatus.CONFLICT),
		CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND),
		CHAT_ROOM_ALREADY_EXISTS(HttpStatus.CONFLICT),
		INVALID_ALERT_RULE(HttpStatus.BAD_REQUEST),
		INVALID_ALERT_SEVERITY(HttpStatus.BAD_REQUEST),
			INVALID_ALERT_CHANNEL(HttpStatus.BAD_REQUEST),
			INVALID_CHAT_ROOM(HttpStatus.BAD_REQUEST),
			INVALID_ALERT_STATUS(HttpStatus.BAD_REQUEST),
			TELEGRAM_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE),
			TELEGRAM_DISCOVERY_FAILED(HttpStatus.BAD_GATEWAY);

		private final HttpStatus status;

		ErrorCode(HttpStatus status) {
			this.status = status;
		}
	}

	private final ErrorCode errorCode;

	public AlertingException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}
}
