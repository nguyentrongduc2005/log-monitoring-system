package com.vdt.log_monitoring.api.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class ApplicationRequest {

	@NotBlank(message = "Application name is required")
	@Size(max = 100, message = "Application name cannot exceed 100 characters")
	private String name;

	@NotBlank(message = "Application display name is required")
	@Size(max = 150, message = "Application display name cannot exceed 150 characters")
	private String displayName;

	@Size(max = 2000, message = "Application description cannot exceed 2000 characters")
	private String description;
}
