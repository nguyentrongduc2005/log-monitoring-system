package com.vdt.log_monitoring.api.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class UpdateUserRequest {
	@NotBlank(message = "Email is required")
	@Email(message = "Email is invalid")
	@Size(max = 320, message = "Email cannot exceed 320 characters")
	private String email;

	@NotBlank(message = "Display name is required")
	@Size(max = 150, message = "Display name cannot exceed 150 characters")
	private String displayName;
}
