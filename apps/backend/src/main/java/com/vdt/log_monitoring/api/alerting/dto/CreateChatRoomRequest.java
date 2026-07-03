package com.vdt.log_monitoring.api.alerting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class CreateChatRoomRequest {

	@NotBlank(message = "Channel is required")
	private String channel;

	@NotBlank(message = "Chat room name is required")
	@Size(max = 120, message = "Chat room name cannot exceed 120 characters")
	private String name;

	@NotBlank(message = "Chat id is required")
	@Size(max = 128, message = "Chat id cannot exceed 128 characters")
	private String chatId;

	@Size(max = 2000, message = "Description cannot exceed 2000 characters")
	private String description;
}
