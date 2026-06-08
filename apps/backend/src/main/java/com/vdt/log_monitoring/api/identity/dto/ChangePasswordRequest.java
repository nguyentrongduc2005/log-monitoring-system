package com.vdt.log_monitoring.api.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class ChangePasswordRequest {
	@NotBlank(message = "Old password is required")
	private String oldPassword;

	@NotBlank(message = "New password is required")
	@Size(min = 6, max = 100, message = "New password must be between 6 and 100 characters")
	private String newPassword;
}
