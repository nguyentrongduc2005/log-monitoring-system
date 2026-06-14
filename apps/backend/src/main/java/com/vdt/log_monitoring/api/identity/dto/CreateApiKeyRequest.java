package com.vdt.log_monitoring.api.identity.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class CreateApiKeyRequest {

	@NotBlank(message = "API key name is required")
	@Size(max = 100, message = "API key name cannot exceed 100 characters")
	private String name;

	private Instant expiresAt;
}
