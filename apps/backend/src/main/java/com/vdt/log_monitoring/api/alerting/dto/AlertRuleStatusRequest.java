package com.vdt.log_monitoring.api.alerting.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class AlertRuleStatusRequest {

	@NotBlank(message = "Alert rule status is required")
	private String status;
}
