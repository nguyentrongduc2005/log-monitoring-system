package com.vdt.log_monitoring.api.identity.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

import com.vdt.log_monitoring.modules.identity.api.ApplicationAccessFacade;

@Data
@Builder
public class ApplicationResponse {
	private UUID id;
	private String name;
	private String displayName;
	private String description;
	private String status;
	private Instant createdAt;
	private Instant updatedAt;

	public static ApplicationResponse from(ApplicationAccessFacade.ApplicationDto dto) {
		return ApplicationResponse.builder()
			.id(dto.id())
			.name(dto.name())
			.displayName(dto.displayName())
			.description(dto.description())
			.status(dto.status())
			.createdAt(dto.createdAt())
			.updatedAt(dto.updatedAt())
			.build();
	}
}
