package com.vdt.log_monitoring.api.identity.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class ApplicationAccessGrantRequest {

	private UUID applicationId;

	@NotBlank(message = "Application access level is required")
	private String accessLevel;
}
