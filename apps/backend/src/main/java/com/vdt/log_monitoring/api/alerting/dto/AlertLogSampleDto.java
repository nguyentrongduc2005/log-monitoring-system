package com.vdt.log_monitoring.api.alerting.dto;

public record AlertLogSampleDto(
		String level,
		String message
) {
}
