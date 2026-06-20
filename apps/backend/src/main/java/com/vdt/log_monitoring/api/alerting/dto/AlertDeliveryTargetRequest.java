package com.vdt.log_monitoring.api.alerting.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class AlertDeliveryTargetRequest {

	@NotBlank(message = "Alert channel is required")
	private String channel;

	@NotNull(message = "Chat room id is required")
	private UUID chatRoomId;
}
