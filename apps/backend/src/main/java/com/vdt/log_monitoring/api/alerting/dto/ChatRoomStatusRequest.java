package com.vdt.log_monitoring.api.alerting.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class ChatRoomStatusRequest {

	@NotBlank(message = "Chat room status is required")
	private String status;
}
