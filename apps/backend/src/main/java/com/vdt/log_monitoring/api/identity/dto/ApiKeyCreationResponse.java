package com.vdt.log_monitoring.api.identity.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

import com.vdt.log_monitoring.modules.identity.api.ApplicationAccessFacade;

@Data
@Builder
public class ApiKeyCreationResponse {
	private UUID id;
	private UUID applicationId;
	private String name;
	private String keyPrefix;
	private String rawApiKey;
	private String status;
	private Instant expiresAt;
	private Instant createdAt;

	public static ApiKeyCreationResponse from(ApplicationAccessFacade.ApiKeyCreationDto dto) {
		return ApiKeyCreationResponse.builder()
			.id(dto.id())
			.applicationId(dto.applicationId())
			.name(dto.name())
			.keyPrefix(dto.keyPrefix())
			.rawApiKey(dto.rawApiKey())
			.status(dto.status())
			.expiresAt(dto.expiresAt())
			.createdAt(dto.createdAt())
			.build();
	}
}
