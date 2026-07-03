package com.vdt.log_monitoring.modules.incident.api;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class IncidentException extends RuntimeException {

	@Getter
	public enum ErrorCode {
		INCIDENT_NOT_FOUND(HttpStatus.NOT_FOUND),
		INVALID_INCIDENT_REQUEST(HttpStatus.BAD_REQUEST),
		INVALID_INCIDENT_STATUS(HttpStatus.BAD_REQUEST),
		INCIDENT_ANALYSIS_FAILED(HttpStatus.BAD_GATEWAY);

		private final HttpStatus status;

		ErrorCode(HttpStatus status) {
			this.status = status;
		}
	}

	private final ErrorCode errorCode;

	public IncidentException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}
}
