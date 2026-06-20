package com.vdt.log_monitoring.api.alerting.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class AlertRuleRequest {

	@NotNull(message = "Application id is required")
	private UUID applicationId;

	@NotBlank(message = "Alert rule name is required")
	@Size(max = 120, message = "Alert rule name cannot exceed 120 characters")
	private String name;

	@Size(max = 2000, message = "Description cannot exceed 2000 characters")
	private String description;

	@NotBlank(message = "Minimum severity is required")
	private String minSeverity;

	@Size(max = 255, message = "Keyword pattern cannot exceed 255 characters")
	private String keywordPattern;

	@Min(value = 1, message = "Threshold count must be positive")
	private int thresholdCount;

	@Min(value = 1, message = "Threshold window must be positive")
	private int thresholdWindowSeconds;

	@Min(value = 1, message = "Cooldown must be positive")
	private int cooldownSeconds;

	private List<String> channels;

	@Valid
	private List<AlertDeliveryTargetRequest> deliveryTargets;
}
