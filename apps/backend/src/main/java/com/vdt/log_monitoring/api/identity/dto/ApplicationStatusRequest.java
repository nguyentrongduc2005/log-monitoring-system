package com.vdt.log_monitoring.api.identity.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class ApplicationStatusRequest {

	@NotBlank(message = "Application status is required")
	private String status;
}
