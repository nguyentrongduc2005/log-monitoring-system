package com.vdt.log_monitoring.api.alerting.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class AlertDeliveryTargetRequest {

	@NotBlank(message = "Alert channel is required")
	private String channel;

	private UUID chatRoomId;
}
