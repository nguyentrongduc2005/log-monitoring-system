package com.vdt.log_monitoring.api.identity.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class ReplaceApplicationAccessRequest {

	@Valid
	@NotNull(message = "Application grants are required")
	private List<ApplicationAccessGrantRequest> grants;
}
